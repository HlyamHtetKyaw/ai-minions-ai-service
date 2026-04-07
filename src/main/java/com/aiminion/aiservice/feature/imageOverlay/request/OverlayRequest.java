package com.aiminion.aiservice.feature.imageOverlay.request;
import lombok.Builder;

/**
 * Describes what images to overlay on the base image.
 * All fields are optional — null means skip that overlay.
 */
@Builder
public record OverlayRequest(
        String baseImageUrl,    // AI-generated image URL
        String logoUrl,         // logo to overlay (nullable)
        String photoUrl,        // person/brand photo to overlay (nullable)

        // Optional overlay config from user
        String  logoPosition,
        Integer logoWidth,
        Integer logoHeight,
        Integer logoMargin,

        String  photoPosition,
        Integer photoWidth,
        Integer photoHeight,
        Integer photoMargin
) {}