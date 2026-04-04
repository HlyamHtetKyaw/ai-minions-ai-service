package com.aiminion.aiservice.common.ai.handler;

import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Each AI feature implements this to plug into the router.
 */
public interface AiFeatureHandler {
    FeatureType getFeatureType();
    AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper);
}
