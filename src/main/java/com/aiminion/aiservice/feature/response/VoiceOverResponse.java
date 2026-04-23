package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

import java.util.List;

@Builder
public record VoiceOverResponse(
        String audioUrl,
        String s3Key,
        byte[] audioByte,
        String audioBase64,
        String sourceLanguage,
        String targetLanguage,
        String style,
        String rawOutput,
        AiProvider usedProvider,
        Integer tokenIn,
        Integer tokenOut,
        List<String> aiLatestModels
) {}
