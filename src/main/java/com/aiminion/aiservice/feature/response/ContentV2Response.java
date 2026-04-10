package com.aiminion.aiservice.feature.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentV2Response(
        ContentTextResponse text,
        ContentImageV2Response image,
        AiProvider usedProvider
) {}
