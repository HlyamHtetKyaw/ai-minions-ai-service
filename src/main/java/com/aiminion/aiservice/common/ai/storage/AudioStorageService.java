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

    
    public String save(byte[] audioBytes) {
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);

            String ext = detectExtension(audioBytes);
            String filename = "vo_" + Instant.now().getEpochSecond() + ext;
            Path filePath = dir.resolve(filename);
            Files.write(filePath, audioBytes);

            String url = baseUrl + "/" + filename;

            log.info("[AudioStorageService] Saved audio at: {}", filePath);
            log.info("[AudioStorageService] Public URL: {}", url);
            return url;

        } catch (IOException ex) {
            log.error("[AudioStorageService] Failed to save audio: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to store generated audio.");
        }
    }

    private String detectExtension(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return ".bin";

        // WAV: RIFFxxxxWAVE
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E') {
            return ".wav";
        }

        // OGG: OggS
        if (bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S') {
            return ".ogg";
        }

        // MP3: "ID3" or frame sync 0xFFEx
        if ((bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                || ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0)) {
            return ".mp3";
        }

        return ".bin";
    }
}