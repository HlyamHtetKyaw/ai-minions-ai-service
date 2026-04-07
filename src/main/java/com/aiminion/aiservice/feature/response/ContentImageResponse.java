package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentImageResponse(
        String imageUrl,
        String imageName,
        String prompt,
        byte[] imageBytes,
        String imageBase64,
        AiProvider usedProvider
) {}