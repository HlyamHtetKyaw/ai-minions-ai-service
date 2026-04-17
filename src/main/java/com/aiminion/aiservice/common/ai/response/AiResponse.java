package com.aiminion.aiservice.common.ai.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record AiResponse(
        String content,
        AiProvider usedProvider,
        Integer tokenIn,
        Integer tokenOut
) {}