package com.aiminion.aiservice.common.ai.response;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import lombok.Builder;

@Builder
public record AiGenerateResponse(
        FeatureType featureType,
        AiProvider usedProvider,
        Object      result          // TranslateResponse, TranscribeResponse, etc.
) {}