package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(
        name = "GenerateImagePayload",
        description = "Payload for generating image content"
)
@Builder
public record ContentImageRequest(

        @Schema(description = "Prompt used to generate image", example = "A peaceful Myanmar village at sunset")
        @NotBlank(message = "Prompt must not be blank.")
        String prompt,

        @Schema(example = "1024x1024", defaultValue = "1024x1024")
        String size,

        @Schema(example = "standard", defaultValue = "standard")
        String quality,

        @Schema(description = "URL of logo to overlay (optional)")
        String logoUrl,         // ← nullable

        @Schema(description = "URL of photo to overlay (optional)")
        String photoUrl,        // ← nullable

        @Schema(example = "OPENAI")
        AiProvider provider

) implements AiPayload {}