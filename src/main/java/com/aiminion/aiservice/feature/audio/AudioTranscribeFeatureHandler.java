package com.aiminion.aiservice.feature.audio;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.aiminion.aiservice.common.ai.client.GoogleGeminiTranscriptionClient;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioTranscribeFeatureHandler implements AiFeatureHandler {

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(30))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private final GoogleGeminiTranscriptionClient googleGeminiTranscriptionClient;

	@Override
	public FeatureType getFeatureType() {
		return FeatureType.AUDIO;
	}

	@Override
	public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
		JsonNode p = request.payload();
		validateTranscribeRequest(request, p);

		String url = p.path("audioPresignedUrl").asText(null);
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException(
					"payload.audioPresignedUrl is required for JSON requests; use multipart /generate with an audio part for direct upload");
		}

		String mimeType = p.path("mimeType").asText("audio/wav");

		log.info("AUDIO transcribe: downloading from presigned URL ({} chars)", url.length());

		byte[] audio = downloadAudio(url);
		return buildResponse(objectMapper, audio, mimeType);
	}

	public AiGenerateResponse handleInlineAudio(
			AiGenerateRequest request,
			byte[] audioBytes,
			String filename,
			String mimeType,
			ObjectMapper objectMapper) {
		if (audioBytes == null || audioBytes.length == 0) {
			throw new IllegalArgumentException("audio bytes must not be empty");
		}
		JsonNode p = request.payload();
		validateTranscribeRequest(request, p);

		String mt = mimeType != null && !mimeType.isBlank() ? mimeType : p.path("mimeType").asText("audio/wav");

		log.info("AUDIO transcribe: inline audio {} bytes", audioBytes.length);
		return buildResponse(objectMapper, audioBytes, mt);
	}

	private void validateTranscribeRequest(AiGenerateRequest request, JsonNode p) {
		String op = p.path("operation").asText("transcribe");
		if (!"transcribe".equalsIgnoreCase(op)) {
			throw new IllegalArgumentException("Unsupported AUDIO operation: " + op + " (supported: transcribe)");
		}
		if (request.provider() != null && request.provider() != AiProvider.GEMINI) {
			throw new IllegalArgumentException("Transcription uses Google AI (Gemini); provider must be GEMINI or omitted");
		}
	}

	private AiGenerateResponse buildResponse(ObjectMapper objectMapper, byte[] audio, String mimeType) {
		String text = googleGeminiTranscriptionClient.transcribe(audio, mimeType);
		ObjectNode result = objectMapper.createObjectNode();
		result.put("text", text);
		return AiGenerateResponse.builder()
				.featureType(FeatureType.AUDIO)
				.usedProvider(AiProvider.GEMINI)
				.result(result)
				.build();
	}

	private static byte[] downloadAudio(String url) {
		try {
			HttpRequest req = HttpRequest.newBuilder(URI.create(url.trim()))
					.timeout(Duration.ofMinutes(15))
					.GET()
					.build();
			HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
			if (res.statusCode() / 100 != 2) {
				throw new IllegalStateException("Failed to download audio: HTTP " + res.statusCode());
			}
			return res.body();
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Failed to download audio from presigned URL: " + e.getMessage(), e);
		}
	}
}
