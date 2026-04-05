package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentGenerateResponse(
        String generatedContent,
        String translatedFrom,
        String translatedTo,
        String style,
        AiProvider usedProvider
) {}