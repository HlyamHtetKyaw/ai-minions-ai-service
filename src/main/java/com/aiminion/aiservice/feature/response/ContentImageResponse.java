package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentImageResponse(
        String imageUrl,
        String imageName,
        String prompt,
        AiProvider usedProvider
) {}