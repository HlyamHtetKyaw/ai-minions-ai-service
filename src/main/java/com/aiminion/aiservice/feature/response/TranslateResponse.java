package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record TranslateResponse(
        String translatedText,
        String translatedFrom,
        String translatedTo,
        String style,
        AiProvider usedProvider
) {}