package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.Builder;

@Builder
public record ContentImageRequest(

        String prompt,

        String size,

        String quality,

        AiProvider provider

) {}