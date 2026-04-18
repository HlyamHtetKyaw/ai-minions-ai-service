package com.aiminion.aiservice.common.ai.storage.impl;

import com.aiminion.aiservice.common.ai.storage.CloudStorageClient;
import com.aiminion.aiservice.common.ai.storage.StoredMedia;
import com.aiminion.aiservice.common.enums.MediaCategory;
import com.aiminion.aiservice.common.enums.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GcpCloudStorageClient implements CloudStorageClient {

    private final RestTemplate restTemplate;

    @Value("${processing.storage.base-url:http://localhost:8082}")
    private String baseUrl;

    // Path templates per media type
    @Value("${processing.storage.image-path:/api/v1/internal/storage/images}")
    private String imagePath;

    @Value("${processing.storage.audio-path:/api/v1/internal/storage/audios}")
    private String audioPath;

    @Value("${processing.storage.video-path:/api/v1/internal/storage/videos}")
    private String videoPath;

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.GCP;
    }

    @Override
    public StoredMedia store(byte[] bytes, MediaCategory category, String fileName) {
        String url = baseUrl.trim() + resolvePath(category);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "fileBytes", bytes,
                "keyHint",   fileName
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (!(response.getBody() instanceof Map<?, ?> payload)) {
                throw new RuntimeException("GCP storage returned empty payload");
            }

            Object storageUrl = payload.get("storageUrl");
            Object key        = payload.get("key");

            if (!(storageUrl instanceof String s) || s.isBlank()) {
                throw new RuntimeException("GCP storage storageUrl is missing");
            }

            String resolvedKey = key instanceof String k ? k : "";
            log.info("[GcpCloudStorageClient] Stored {} → url={} key={}", category, s, resolvedKey);
            return new StoredMedia(s, resolvedKey);

        } catch (Exception ex) {
            log.error("[GcpCloudStorageClient] Failed to store {} file={}: {}", category, fileName, ex.getMessage(), ex);
            throw new RuntimeException("GCP storage unavailable. Please try again later.");
        }
    }

    private String resolvePath(MediaCategory category) {
        return switch (category) {
            case IMAGE -> imagePath;
            case AUDIO -> audioPath;
            case VIDEO -> videoPath;
        };
    }
}