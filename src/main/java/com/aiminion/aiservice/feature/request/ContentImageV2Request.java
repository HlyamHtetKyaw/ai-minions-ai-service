package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
//import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(
        name = "GenerateImageV2Payload",
        description = "Payload for V2 image generation with toon style and short text overlay"
)
@Builder
public record ContentImageV2Request(

        @Schema(description = "Prompt used to generate image", example = "A developer handling a production incident")
        @NotBlank(message = "Prompt must not be blank.")
        String prompt,

        @Schema(example = "1024x1024", defaultValue = "1024x1024")
        String size,

        @Schema(description = "Short text rendered on top of generated image", example = "All tests passed")
        String shortText,

        @Schema(description = "When true, generate and render AI caption overlay")
        Boolean aiOverlayTextEnabled,

        @Schema(description = "Optional user-provided overlay text")
        String userOverlayText,

        @Schema(description = "Visual style. Leave blank to use toon defaults.", example = "toon comic style, cinematic lighting")
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
        Integer photoMargin,

        @Schema(description = "Social copy format (hook, caption, hashtags, script) — steers scene intent for the image")
        String contentType,

        @Schema(description = "Voice / mood (matches text leg: inspiring, funny, …) — steers atmosphere and energy")
        String tone
){}
