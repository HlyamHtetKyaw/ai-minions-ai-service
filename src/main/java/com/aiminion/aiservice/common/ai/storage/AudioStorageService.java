package com.aiminion.aiservice.common.ai.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

@Slf4j
@Service
public class AudioStorageService {

    @Value("${audio.storage.path:/tmp/aiminion/audio}")
    private String storagePath;

    @Value("${audio.storage.base-url:http://localhost:8080/audio}")
    private String baseUrl;

    /**
     * Saves MP3 bytes to disk and returns a publicly accessible URL.
     */
    public String save(byte[] audioBytes) {
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);

            String filename = "vo_" + Instant.now().getEpochSecond() + ".mp3";
            Path filePath = dir.resolve(filename);
            Files.write(filePath, audioBytes);

            String url = baseUrl + "/" + filename;
            log.info("[AudioStorageService] Saved audio: {}", url);
            return url;

        } catch (IOException ex) {
            log.error("[AudioStorageService] Failed to save audio: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to store generated audio.");
        }
    }
}