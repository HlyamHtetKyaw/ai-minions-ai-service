package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentRequest(

        String topic,

        String sourceLanguage,

        String targetLanguage,

        String style,

        String imageSize,

        String imageQuality,

        AiProvider provider

) {}