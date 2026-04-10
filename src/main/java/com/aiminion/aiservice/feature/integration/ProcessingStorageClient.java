package com.aiminion.aiservice.feature.integration;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingStorageClient {

	private final RestTemplate restTemplate;

	@Value("${processing.storage.base-url:http://localhost:8082}")
	private String processingBaseUrl;

	@Value("${processing.storage.image-path:/api/v1/internal/storage/images}")
	private String processingImagePath;

	public StoredImage storeImage(byte[] imageBytes, String keyHint) {
		String url = processingBaseUrl.trim() + processingImagePath.trim();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> body = Map.of(
				"imageBytes", imageBytes,
				"keyHint", keyHint == null ? "" : keyHint);
		ResponseEntity<?> response = restTemplate.postForEntity(
				url,
				new HttpEntity<>(body, headers),
				Map.class);
		if (!(response.getBody() instanceof Map<?, ?> payload)) {
			throw new RuntimeException("Processing service returned empty storage payload");
		}
		Object storageUrl = payload.get("storageUrl");
		Object key = payload.get("key");
		if (!(storageUrl instanceof String s) || s.isBlank()) {
			throw new RuntimeException("Processing service storageUrl is missing");
		}
		String resolvedKey = key instanceof String k ? k : "";
		log.info("[ProcessingStorageClient] Stored image url={} key={}", s, resolvedKey);
		return new StoredImage(s, resolvedKey);
	}

	public record StoredImage(
			String storageUrl,
			String key
	) {
	}
}
