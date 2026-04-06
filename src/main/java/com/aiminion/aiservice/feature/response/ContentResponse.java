package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentResponse(
        ContentTextResponse  text,
        ContentImageResponse image,
        byte[] imageBytes,
        AiProvider usedProvider
) {}