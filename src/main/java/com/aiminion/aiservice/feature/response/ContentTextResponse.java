package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentTextResponse(
        String generatedContent,
        String generatedFrom,
        String generatedTo,
        String style,
        AiProvider usedProvider
) {}