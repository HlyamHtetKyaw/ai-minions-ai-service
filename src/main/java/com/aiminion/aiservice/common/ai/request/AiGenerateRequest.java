package com.aiminion.aiservice.common.ai.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AiGenerateRequest(

        @NotNull(message = "featureType must not be null.")
        FeatureType featureType,

        // Provider is optional — null means use default
        AiProvider provider,

        // Feature-specific payload — each feature deserializes this differently
        @NotNull(message = "payload must not be null.")
        JsonNode payload

) {}
