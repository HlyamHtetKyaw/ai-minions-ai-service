package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentGenerateRequest(

        String topic,

        String sourceLanguage,  // defaults to "English"
        String targetLanguage,  // defaults to "Myanmar"
        String style,           // defaults to "Formal"
        AiProvider provider     // null = use default
) {}
