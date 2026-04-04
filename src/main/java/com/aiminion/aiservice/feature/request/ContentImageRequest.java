package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ContentImageRequest(

        @NotBlank(message = "Prompt must not be blank.")
        String prompt,

        String size,
        String quality,
        AiProvider provider
) {}