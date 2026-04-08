package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record TranslateRequest(

        String text,

        String sourceLanguage,

        String targetLanguage,

        String style,

        AiProvider provider

) {}