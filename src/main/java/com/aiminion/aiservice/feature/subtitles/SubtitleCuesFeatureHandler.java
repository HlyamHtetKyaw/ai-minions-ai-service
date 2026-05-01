package com.aiminion.aiservice.feature.subtitles;

import com.aiminion.aiservice.common.ai.client.GoogleGeminiSubtitleCuesClient;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.handler.InlineAudioFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubtitleCuesFeatureHandler implements AiFeatureHandler, InlineAudioFeatureHandler {

	private final GoogleGeminiSubtitleCuesClient googleGeminiSubtitleCuesClient;

	@Override
	public FeatureType getFeatureType() {
		return FeatureType.SUBTITLES;
	}

	@Override
	public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
		JsonNode p = request.payload();
		String op = p.path("operation").asText("subtitles_srt_cues");
		if (!"subtitles_srt_refine".equalsIgnoreCase(op)) {
			throw new IllegalArgumentException(
					"SUBTITLES JSON mode supports only subtitles_srt_refine (use multipart for subtitles_srt_cues)");
		}
		validateProvider(request);

		String srtText = p.path("srtText").asText("");
		String translatedText = p.path("translatedText").asText("");
		String targetLanguage = p.path("targetLanguage").asText("my");
		String styleProfile = resolveStyleProfile(p);
		var refined = googleGeminiSubtitleCuesClient.refineSrt(srtText, translatedText, targetLanguage, styleProfile);

		ObjectNode result = objectMapper.createObjectNode();
		result.put("srtText", refined.srtText());
		if (refined.promptTokens() != null) {
			result.put("tokenIn", refined.promptTokens());
		}
		if (refined.completionTokens() != null) {
			result.put("tokenOut", refined.completionTokens());
		}
		return AiGenerateResponse.builder()
				.featureType(FeatureType.SUBTITLES)
				.usedProvider(AiProvider.GEMINI)
				.result(result)
				.build();
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
		validateInlineRequest(request, p);

		long chunkDurationMs = p.path("chunkDurationMs").asLong(25_000);
		if (chunkDurationMs <= 0) {
			chunkDurationMs = 25_000;
		}
		long chunkOffsetMs = p.path("chunkOffsetMs").asLong(0);
		if (chunkOffsetMs < 0) {
			chunkOffsetMs = 0;
		}
		int chunkIndex = p.path("chunkIndex").asInt(0);
		if (chunkIndex < 0) {
			chunkIndex = 0;
		}
		String targetLanguage = p.path("targetLanguage").asText("my");
		String styleProfile = resolveStyleProfile(p);

		String mt = mimeType != null && !mimeType.isBlank() ? mimeType : p.path("mimeType").asText("audio/wav");

		log.info("SUBTITLES: inline audio {} bytes chunkIndex={} chunkOffsetMs={} chunkDurationMs={} targetLanguage={} styleProfile={}",
				audioBytes.length, chunkIndex, chunkOffsetMs, chunkDurationMs, targetLanguage, styleProfile);
		var r = googleGeminiSubtitleCuesClient.generateCues(
				audioBytes,
				mt,
				chunkDurationMs,
				chunkOffsetMs,
				chunkIndex,
				targetLanguage,
				styleProfile);

		ArrayNode cuesArray;
		try {
			JsonNode parsed = objectMapper.readTree(r.cuesJsonArray());
			cuesArray = parsed != null && parsed.isArray() ? (ArrayNode) parsed : objectMapper.createArrayNode();
		} catch (Exception e) {
			cuesArray = objectMapper.createArrayNode();
		}

		ObjectNode result = objectMapper.createObjectNode();
		result.set("cues", cuesArray);
		if (r.promptTokens() != null) {
			result.put("tokenIn", r.promptTokens());
		}
		if (r.completionTokens() != null) {
			result.put("tokenOut", r.completionTokens());
		}

		return AiGenerateResponse.builder()
				.featureType(FeatureType.SUBTITLES)
				.usedProvider(AiProvider.GEMINI)
				.result(result)
				.build();
	}

	private void validateInlineRequest(AiGenerateRequest request, JsonNode p) {
		String op = p.path("operation").asText("subtitles_srt_cues");
		if (!"subtitles_srt_cues".equalsIgnoreCase(op)) {
			throw new IllegalArgumentException("Unsupported SUBTITLES operation: " + op + " (supported: subtitles_srt_cues)");
		}
		validateProvider(request);
	}

	private void validateProvider(AiGenerateRequest request) {
		if (request.provider() != null && request.provider() != AiProvider.GEMINI) {
			throw new IllegalArgumentException("SUBTITLES uses Google AI (Gemini); provider must be GEMINI or omitted");
		}
	}

	private static String resolveStyleProfile(JsonNode p) {
		String style = p.path("styleProfile").asText("");
		if (style.isBlank()) {
			style = p.path("style").asText("");
		}
		style = style == null ? "" : style.trim();
		if ("toon_caption_rules".equalsIgnoreCase(style)) {
			return "caption_rules_v1";
		}
		return style.isBlank() ? "caption_rules_v1" : style;
	}
}

