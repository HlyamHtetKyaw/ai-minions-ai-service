package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ContentRequest(

        @NotBlank(message = "Topic must not be blank.")
        @Size(max = 1500, message = "Topic must not exceed 1500 characters.")
        String topic,           // used for both text generation and image prompt

        String sourceLanguage,
        String targetLanguage,
        String style,
        String imageSize,       // defaults to "1024x1024"
        String imageQuality,    // defaults to "standard"
        AiProvider provider
) {}