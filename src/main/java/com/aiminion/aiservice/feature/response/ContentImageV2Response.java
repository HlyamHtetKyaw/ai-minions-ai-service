package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentImageV2Response(
        String imageName,
        String prompt,
        String shortText,
        byte[] imageBytes,
        String imageBase64,
        String storageUrl,
        String s3Key,
        AiProvider usedProvider
) {}
