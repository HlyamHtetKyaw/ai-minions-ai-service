package com.aiminion.aiservice.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MediaFileNameGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    // Sequence resets on app restart — swap with DB sequence if persistence needed
    private final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Generates: David_012_AiVoiceOver_001_20260417_120816.mp3
     *
     * @param username   e.g. "David"
     * @param userId     e.g. "012"
     * @param fileLabel  e.g. "AiVoiceOver", "AiImage"
     * @param extension  e.g. "mp3", "png", "mp4"
     */
    public String generate(String username, String userId, String fileLabel, String extension) {
        LocalDateTime now = LocalDateTime.now();

        String seq  = String.format("%03d", sequence.incrementAndGet());
        String date = now.format(DATE_FMT);
        String time = now.format(TIME_FMT);

        String safeName   = sanitize(username);
        String safeId     = sanitize(userId);
        String safeLabel  = sanitize(fileLabel);

        return String.format("%s_%s_%s_%s_%s_%s.%s",
                safeName, safeId, safeLabel, seq, date, time, extension);
        // → David_012_AiVoiceOver_001_20260417_120816.mp3
    }

    private String sanitize(String value) {
        return (value == null || value.isBlank()) ? "unknown"
                : value.trim().replaceAll("[^a-zA-Z0-9]", "");
    }
}