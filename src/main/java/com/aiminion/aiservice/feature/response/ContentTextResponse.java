package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentTextResponse(
        String title,
        String contentType,
        String generatedFrom,
        String generatedTo,
        String style,
        AiProvider usedProvider,
        String generatedContent
) {}