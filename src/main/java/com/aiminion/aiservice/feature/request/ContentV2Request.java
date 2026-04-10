package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(
        name = "GenerateContentV2Payload",
        description = "Payload for V2 content generation (image + text)"
)
@Builder
public record ContentV2Request(

        @Schema(description = "Topic for text and image generation", example = "Testing vs production environment")
        @NotBlank(message = "Topic must not be blank.")
        String topic,

        @Schema(example = "Caption", defaultValue = "Caption")
        String contentType,

        @Schema(example = "English", defaultValue = "English")
        String sourceLanguage,

        @Schema(example = "Myanmar", defaultValue = "Myanmar")
        String targetLanguage,

        @Schema(example = "Funny", defaultValue = "Funny")
        String style,

        @Schema(example = "SHORT", defaultValue = "SHORT", description = "Desired generated text length: SHORT or LONG")
        String textLength,

        @Schema(example = "1024x1024", defaultValue = "1024x1024")
        String imageSize,

        @Schema(description = "Optional short text rendered on image; if missing, uses generated title")
        String shortText,

        @Schema(description = "When true, generate and render AI caption overlay")
        Boolean aiOverlayTextEnabled,

        @Schema(description = "Optional user-provided overlay text")
        String userOverlayText,

        @Schema(description = "Visual style instructions for image generation", example = "toon comic style, meme layout")
        String toonStyle,

        @Schema(description = "URL or data URI (data:image/...;base64,...) for logo overlay")
        String logoUrl,

        @Schema(description = "Optional URL or data URI for extra photo overlay")
        String photoUrl,

        @Schema(example = "GEMINI")
        AiProvider provider,

        String logoPosition,
        Integer logoWidth,
        Integer logoHeight,
        Integer logoMargin,

        String photoPosition,
        Integer photoWidth,
        Integer photoHeight,
        Integer photoMargin
) implements AiPayload {}
