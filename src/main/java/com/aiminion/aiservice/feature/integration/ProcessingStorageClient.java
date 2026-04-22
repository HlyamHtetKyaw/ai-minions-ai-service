package com.aiminion.aiservice.feature.integration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	@Value("${processing.storage.base-url:http://localhost:8080}")
	private String processingBaseUrl;

	@Value("${processing.storage.image-path:/api/v1/internal/storage/images}")
	private String processingImagePath;

	@Value("${processing.storage.audio-path:/api/v1/internal/storage/audio}")
	private String processingAudioPath;

	@Value("${processing.storage.local-audio-path:D:/Personal/voice-over-audio}")
	private String localAudioPath;

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

	public StoredAudio storeAudio(byte[] audioBytes, String keyHint, String contentType) {
		String url = processingBaseUrl.trim() + processingAudioPath.trim();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> body = Map.of(
				"audioBytes", audioBytes,
				"keyHint", keyHint == null ? "" : keyHint,
				"contentType", contentType == null ? "audio/mpeg" : contentType);
		ResponseEntity<?> response = restTemplate.postForEntity(
				url,
				new HttpEntity<>(body, headers),
				Map.class);
		if (!(response.getBody() instanceof Map<?, ?> payload)) {
			throw new RuntimeException("Processing service returned empty audio storage payload");
		}
		Object storageUrl = payload.get("storageUrl");
		Object key = payload.get("key");
		if (!(storageUrl instanceof String s) || s.isBlank()) {
			throw new RuntimeException("Processing service audio storageUrl is missing");
		}
		String resolvedKey = key instanceof String k ? k : "";
		log.info("[ProcessingStorageClient] Stored audio url={} key={}", s, resolvedKey);
		return new StoredAudio(s, resolvedKey);
	}

	public StoredAudio storeAudioLocally(byte[] audioBytes, String keyHint, String contentType) {
		try {
			Path basePath = Paths.get(localAudioPath).toAbsolutePath().normalize();

			Path dirPath = basePath.resolve("voice-over");

			Files.createDirectories(dirPath);

			String safeFileName = (keyHint == null || keyHint.isBlank()) ? "audio" : keyHint;

			safeFileName = Paths.get(safeFileName).getFileName().toString();

			if (safeFileName.contains(".")) {
				safeFileName = safeFileName.substring(0, safeFileName.lastIndexOf("."));
			}

			String extension = resolveExtension(contentType);

			String finalFileName = safeFileName + "_" + System.currentTimeMillis() + extension;

			Path filePath = dirPath.resolve(finalFileName);

			Files.write(filePath, audioBytes);

			String storageUrl = filePath.toUri().toString();

			log.info("[ProcessingStorageClient] Stored audio locally url={} key={}", storageUrl, finalFileName);

			return new StoredAudio(storageUrl, finalFileName);

		} catch (IOException e) {
			log.error("Failed to store audio locally", e);
			throw new RuntimeException("Local audio storage failed", e);
		}
	}

	private String resolveExtension(String contentType) {
		if (contentType == null) return ".mp3";

		return switch (contentType) {
			case "audio/mpeg" -> ".mp3";
			case "audio/wav" -> ".wav";
			case "audio/ogg" -> ".ogg";
			default -> ".mp3";
		};
	}

	public record StoredImage(
			String storageUrl,
			String key
	) {
	}

	public record StoredAudio(
			String storageUrl,
			String key
	) {
	}
}
