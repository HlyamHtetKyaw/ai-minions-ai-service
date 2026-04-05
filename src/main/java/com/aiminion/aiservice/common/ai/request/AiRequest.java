package com.aiminion.aiservice.common.ai.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record AiRequest(
        String systemPrompt,
        String userMessage,
        AiProvider provider    // null = use default
) {}