package com.aiminion.aiservice.feature.imageOverlay.response;

import lombok.Builder;

@Builder
public record OverlayResult(
        byte[] imageBytes,      // final composed image as bytes
        String imageName        // generated filename
) {}