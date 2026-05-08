package com.aiminion.aiservice.common.ai.client;

import java.util.List;

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

import com.aiminion.aiservice.common.ai.voice.VoiceDictationStyleTranscribePrompts;
import com.aiminion.aiservice.config.GoogleAiProperties;
import com.aiminion.aiservice.common.ai.clientProvider.GeminiRuntimeChatModelFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleGeminiTranscriptionClient {

	private static final int MAX_RETRIES = 3;
	private static final String TRANSCRIBE_MODEL_DEFAULT = "gemini-flash-lite-latest";

	private final ChatModel chatModel;
	private final GoogleAiProperties googleAiProperties;
	private final GeminiRuntimeChatModelFactory geminiRuntimeChatModelFactory;

	public TranscriptionResult transcribe(byte[] audioBytes, String mimeType) {
		if (audioBytes == null || audioBytes.length == 0) {
			throw new IllegalArgumentException("audio bytes must not be empty");
		}
		String model = googleAiProperties.getTranscribeModel() != null
				? googleAiProperties.getTranscribeModel().trim()
				: "";
		if (model.isBlank()) {
			model = TRANSCRIBE_MODEL_DEFAULT;
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

		String fullText = VoiceDictationStyleTranscribePrompts.fullUserTextPrompt();
		var userMessage = UserMessage.builder()
				.text(fullText)
				.media(List.of(media))
				.build();

		int estimatedTokens = Math.max(2000, Math.min(audioBytes.length / 50, 60_000));
		int dynamicMaxTokens = Math.max(500, Math.min(estimatedTokens, 60_000));

		ChatResponse response = null;
		Exception lastException = null;
		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				log.info("Gemini transcribe attempt {}/{} model='{}' maxOutputTokens={} bytes={}",
						attempt + 1, MAX_RETRIES, model, dynamicMaxTokens, audioBytes.length);
				var options = GoogleGenAiChatOptions.builder()
						.model(model)
						.maxOutputTokens(dynamicMaxTokens)
						.temperature(0.0)
						.build();
				response = geminiRuntimeChatModelFactory.resolve(chatModel).call(new Prompt(userMessage, options));
				break;
			} catch (Exception e) {
				lastException = e;
				log.warn("Gemini transcribe attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES, e.getMessage());
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
					lastException != null ? lastException.getMessage() : "Unknown Gemini transcription error",
					lastException);
		}
		String rawOutput = response.getResult().getOutput().getText();
		String text = VoiceDictationStyleTranscribePrompts.extractContent(rawOutput);

		Integer promptTokens = null;
		Integer completionTokens = null;
		try {
			Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
			if (usage != null) {
				promptTokens = usage.getPromptTokens();
				completionTokens = usage.getCompletionTokens();
			}
		} catch (Exception ignored) {
			// Usage can be absent depending on provider/model/runtime; transcription should still succeed.
		}

		return new TranscriptionResult(text, promptTokens, completionTokens);
	}

	public record TranscriptionResult(
			String text,
			Integer promptTokens,
			Integer completionTokens
	) {}
}
