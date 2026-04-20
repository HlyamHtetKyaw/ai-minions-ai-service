package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record VoiceOverRequest(

        String text,

        String aiModel,

        String sourceLanguage,

        String targetLanguage,

        String style,

        String textLength,

        String username,

        String userId,

        AiProvider provider
) {}
