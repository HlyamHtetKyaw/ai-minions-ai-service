package com.aiminion.aiservice.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class FileNameGenerator {

    public String generate(String username, String userId, String fileName) {
        String sequence = UUID.randomUUID().toString().substring(0, 6);

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));

        return String.format("%s_%s_%s_%s_%s_%s",
                username,
                userId,
                fileName,
                sequence,
                date,
                time
        );
    }
}