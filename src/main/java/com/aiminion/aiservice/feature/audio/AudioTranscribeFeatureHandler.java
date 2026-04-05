package com.aiminion.aiservice.feature.audio;

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

	private final GoogleGeminiTranscriptionClient googleGeminiTranscriptionClient;

	@Override
	public FeatureType getFeatureType() {
		return FeatureType.AUDIO;
	}

	@Override
	public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
		throw new IllegalArgumentException(
				"AUDIO transcribe is only supported via multipart POST /generate with `request` (JSON) and `audio` (binary) parts.");
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
}
