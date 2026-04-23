package com.aiminion.aiservice.feature.audio;

import org.springframework.stereotype.Component;

import com.aiminion.aiservice.common.ai.client.GoogleGeminiTranscriptionClient;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.handler.InlineAudioFeatureHandler;
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
public class AudioTranscribeFeatureHandler implements AiFeatureHandler, InlineAudioFeatureHandler {

	private final GoogleGeminiTranscriptionClient googleGeminiTranscriptionClient;

	@Override
	public FeatureType getFeatureType() {
		return FeatureType.TRANSCRIBE;
	}

	@Override
	public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
		throw new IllegalArgumentException(
				"AUDIO transcribe is only supported via multipart POST /generate with `request` (JSON) and `audio` (binary) parts.");
	}

	@Override
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

		log.info("TRANSCRIBE: inline audio {} bytes", audioBytes.length);
		return buildResponse(objectMapper, audioBytes, mt);
	}

	private void validateTranscribeRequest(AiGenerateRequest request, JsonNode p) {
		String op = p.path("operation").asText("transcribe");
		if (!"transcribe".equalsIgnoreCase(op)) {
			throw new IllegalArgumentException("Unsupported TRANSCRIBE operation: " + op + " (supported: transcribe)");
		}
		if (request.provider() != null && request.provider() != AiProvider.GEMINI) {
			throw new IllegalArgumentException("TRANSCRIBE uses Google AI (Gemini); provider must be GEMINI or omitted");
		}
	}

	private AiGenerateResponse buildResponse(ObjectMapper objectMapper, byte[] audio, String mimeType) {
		var tr = googleGeminiTranscriptionClient.transcribe(audio, mimeType);
		ObjectNode result = objectMapper.createObjectNode();
		result.put("text", tr.text());
		if (tr.promptTokens() != null) {
			result.put("tokenIn", tr.promptTokens());
		}
		if (tr.completionTokens() != null) {
			result.put("tokenOut", tr.completionTokens());
		}
		return AiGenerateResponse.builder()
				.featureType(FeatureType.TRANSCRIBE)
				.usedProvider(AiProvider.GEMINI)
				.result(result)
				.build();
	}
}
