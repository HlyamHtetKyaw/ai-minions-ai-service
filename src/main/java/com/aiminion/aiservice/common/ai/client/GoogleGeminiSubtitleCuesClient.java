package com.aiminion.aiservice.common.ai.client;

import com.aiminion.aiservice.common.ai.voice.SubtitleCuesPrompts;
import com.aiminion.aiservice.config.GoogleAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleGeminiSubtitleCuesClient {

	private static final int MAX_RETRIES = 3;
	private static final String MODEL_DEFAULT = "gemini-flash-lite-latest";

	private final ChatModel chatModel;
	private final GoogleAiProperties googleAiProperties;

	public SubtitleCuesResult generateCues(
			byte[] audioBytes,
			String mimeType,
			long chunkDurationMs,
			long chunkOffsetMs,
			int chunkIndex,
			String targetLanguage,
			String styleProfile
	) {
		if (audioBytes == null || audioBytes.length == 0) {
			throw new IllegalArgumentException("audio bytes must not be empty");
		}
		String model = googleAiProperties.getTranscribeModel() != null
				? googleAiProperties.getTranscribeModel().trim()
				: "";
		if (model.isBlank()) {
			model = MODEL_DEFAULT;
		}
		String mt = mimeType != null && !mimeType.isBlank() ? mimeType : "audio/wav";

		var audioResource = new ByteArrayResource(audioBytes) {
			@Override
			public String getFilename() {
				return "audio";
			}
		};
		var mimeTypeObj = MimeTypeUtils.parseMimeType(mt);
		var media = new Media(mimeTypeObj, audioResource);

		String fullText = SubtitleCuesPrompts.userPromptSubtitleCues(
				chunkDurationMs,
				chunkOffsetMs,
				chunkIndex,
				targetLanguage,
				styleProfile);
		var userMessage = UserMessage.builder()
				.text(fullText)
				.media(List.of(media))
				.build();

		int dynamicMaxTokens = 16000;

		ChatResponse response = null;
		Exception lastException = null;
		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				log.info("Gemini subtitles attempt {}/{} model='{}' maxOutputTokens={} bytes={}",
						attempt + 1, MAX_RETRIES, model, dynamicMaxTokens, audioBytes.length);
				var options = GoogleGenAiChatOptions.builder()
						.model(model)
						.maxOutputTokens(dynamicMaxTokens)
						.temperature(0.0)
						.build();
				response = chatModel.call(new Prompt(userMessage, options));
				break;
			} catch (Exception e) {
				lastException = e;
				log.warn("Gemini subtitles attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES, e.getMessage());
				if (attempt < MAX_RETRIES - 1) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException("Interrupted during Gemini retry", ie);
					}
				}
			}
		}
		if (response == null) {
			if (lastException instanceof RuntimeException re) {
				throw re;
			}
			throw new IllegalStateException(
					lastException != null ? lastException.getMessage() : "Unknown Gemini subtitles error",
					lastException);
		}

		String rawOutput = response.getResult().getOutput().getText();
		String jsonArrayText = SubtitleCuesPrompts.extractJsonArray(rawOutput);

		Integer promptTokens = null;
		Integer completionTokens = null;
		try {
			Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
			if (usage != null) {
				promptTokens = usage.getPromptTokens();
				completionTokens = usage.getCompletionTokens();
			}
		} catch (Exception ignored) {
			// ignore
		}

		return new SubtitleCuesResult(jsonArrayText, promptTokens, completionTokens);
	}

	public record SubtitleCuesResult(
			String cuesJsonArray,
			Integer promptTokens,
			Integer completionTokens
	) {}
}

