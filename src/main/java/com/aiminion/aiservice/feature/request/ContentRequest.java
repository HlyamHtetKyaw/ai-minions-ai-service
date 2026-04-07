package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(
        name = "GenerateContentPayload",
        description = "Payload for generating both text and image content"
)
@Builder
public record ContentRequest(

        @Schema(description = "Topic for both text and image generation", example = "Importance of sleep")
        @NotBlank(message = "Topic must not be blank.")
        @Size(max = 1500, message = "Topic must not exceed 1500 characters.")
        String topic,

        @Schema(description = "Type of content to generate", example = "Script")
        String contentType,

        @Schema(example = "English", defaultValue = "English")
        String sourceLanguage,

        @Schema(example = "Myanmar", defaultValue = "Myanmar")
        String targetLanguage,

        @Schema(example = "Formal", defaultValue = "Formal")
        String style,

        @Schema(example = "1024x1024", defaultValue = "1024x1024")
        String imageSize,

        @Schema(example = "standard", defaultValue = "standard")
        String imageQuality,

        @Schema(description = "URL of logo to overlay (optional)")
        String logoUrl,         // ← nullable

        @Schema(description = "URL of photo to overlay (optional)")
        String photoUrl,        // ← nullable

        @Schema(example = "OPENAI")
        AiProvider provider,

        // Optional overlay config from user
        String  logoPosition,
        Integer logoWidth,
        Integer logoHeight,
        Integer logoMargin,

        String  photoPosition,
        Integer photoWidth,
        Integer photoHeight,
        Integer photoMargin

) implements AiPayload {}