package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

import java.util.List;

@Builder
public record TranslateResponse(
        String translatedText,
        String translatedFrom,
        String translatedTo,
        String style,
        AiProvider usedProvider,
        Integer tokenIn,
        Integer tokenOut,
        List<String> aiLatestModels,
        List<String> styles
) {}