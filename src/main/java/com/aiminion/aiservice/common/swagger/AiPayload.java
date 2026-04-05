package com.aiminion.aiservice.common.swagger;

import com.aiminion.aiservice.feature.request.ContentImageRequest;
import com.aiminion.aiservice.feature.request.ContentRequest;
import com.aiminion.aiservice.feature.request.ContentTextRequest;
import com.aiminion.aiservice.feature.request.TranslateRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Base payload for AI features",
        oneOf = {
                TranslateRequest.class,
                ContentTextRequest.class,
                ContentImageRequest.class,
                ContentRequest.class
        }
)
public interface AiPayload {
}
