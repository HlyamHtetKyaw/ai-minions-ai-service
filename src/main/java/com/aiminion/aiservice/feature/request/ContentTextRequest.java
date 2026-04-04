package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ContentTextRequest(

        @NotBlank(message = "Topic must not be blank.")
        @Size(max = 1500, message = "Topic must not exceed 1500 characters.")
        String topic,

        String sourceLanguage,  // defaults to "English"
        String targetLanguage,  // defaults to "Myanmar"
        String style,           // defaults to "Formal"
        AiProvider provider
) {}