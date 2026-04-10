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

        /** SHORT or LONG — controls depth while keeping {@link #contentType} (hook, caption, script, …). */
        String textLength,

        AiProvider provider

) {}