package com.aiminion.aiservice.common.ai.generator;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiImagenImageGenerator {

	private final RestTemplate restTemplate;

	@Value("${spring.ai.google.genai.api-key:}")

	private String apiKeyFromSpringAi;

	@Value("${gemini.api-key:}")
	private String apiKeyFromLegacy;

	@Value("${gemini.imagen.api-url:https://generativelanguage.googleapis.com/v1beta/models/}")
	private String imagenApiUrl;

	@Value("${gemini.imagen.model:imagen-4.0-generate-001}")
	private String imagenModel;

	@SuppressWarnings("unchecked")
	public String generateImageDataUrl(String prompt, String aspectRatio, String imageSize) {
		String key = pickApiKey();
		if (key.isBlank()) {
			throw new IllegalStateException("Gemini API key is not configured (set spring.ai.google.genai.api-key or gemini.api-key)");
		}
		String model = imagenModel != null && !imagenModel.isBlank() ? imagenModel.trim() : "imagen-4.0-generate-001";
		String url = imagenApiUrl + model + ":predict";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-goog-api-key", key);

		Map<String, Object> instance = Map.of("prompt", prompt);
		Map<String, Object> params = new java.util.LinkedHashMap<>();
		params.put("sampleCount", 1);
		if (aspectRatio != null && !aspectRatio.isBlank()) {
			params.put("aspectRatio", aspectRatio);
		}
		if (imageSize != null && !imageSize.isBlank()) {
			params.put("imageSize", imageSize);
		}

		Map<String, Object> body = Map.of(
				"instances", List.of(instance),
				"parameters", params
		);

		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
			Map<?, ?> root = response.getBody();
			if (root == null) {
				throw new IllegalStateException("Empty response from Gemini Imagen");
			}
			Object predsObj = root.get("predictions");
			if (!(predsObj instanceof List<?> preds) || preds.isEmpty()) {
				throw new IllegalStateException("Gemini Imagen response missing predictions");
			}
			Object first = preds.get(0);
			if (!(first instanceof Map<?, ?> firstMap)) {
				throw new IllegalStateException("Gemini Imagen response invalid prediction object");
			}
			Object bytesObj = firstMap.get("bytesBase64Encoded");
			if (!(bytesObj instanceof String b64) || b64.isBlank()) {
				log.error("[GeminiImagen] Unexpected response keys: {}", firstMap.keySet());
				throw new IllegalStateException("Gemini Imagen response missing bytesBase64Encoded");
			}
			Base64.getDecoder().decode(b64);
			return "data:image/png;base64," + b64.trim();
		} catch (Exception ex) {
			log.error("[GeminiImagen] Call failed: {}", ex.getMessage(), ex);
			throw new RuntimeException("Gemini image generation service unavailable. Please try again later.");
		}
	}

	private String pickApiKey() {
		String k = apiKeyFromSpringAi != null ? apiKeyFromSpringAi.trim() : "";
		if (!k.isBlank()) return k;
		return apiKeyFromLegacy != null ? apiKeyFromLegacy.trim() : "";
	}
}

