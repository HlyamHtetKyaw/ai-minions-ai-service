package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TranslateRequest(

        @NotBlank(message = "Text must not be blank.")
        @Size(max = 1500, message = "Text must not exceed 1500 characters.")
        String text,

        String sourceLanguage,  // defaults to "English"
        String targetLanguage,  // defaults to "Myanmar"
        String style,           // defaults to "Formal"
        AiProvider provider     // null = use default

) {}