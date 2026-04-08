package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentTextRequest(

        String topic,

        String contentType,

        String sourceLanguage,

        String targetLanguage,

        String style,

        AiProvider provider

) {}