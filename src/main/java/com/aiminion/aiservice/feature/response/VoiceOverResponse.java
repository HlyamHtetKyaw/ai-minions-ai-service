package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record VoiceOverResponse(
        String audioUrl,
        byte[] audioByte,
        String sourceLanguage,
        String targetLanguage,
        String style,
        String rawOutput,
        AiProvider usedProvider
) {}
