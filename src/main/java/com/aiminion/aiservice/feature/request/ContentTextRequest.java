package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(
        name = "GenerateTextPayload",
        description = "Payload for generating text content"
)
@Builder
public record ContentTextRequest(

        @Schema(description = "Topic or prompt for text generation", example = "Importance of sleep")
        @NotBlank(message = "Topic must not be blank.")
        @Size(max = 1500, message = "Topic must not exceed 1500 characters.")
        String topic,

        @Schema(description = "Type of content to generate", example = "Caption")
        String contentType,

        @Schema(example = "English", defaultValue = "English")
        String sourceLanguage,

        @Schema(example = "Myanmar", defaultValue = "Myanmar")
        String targetLanguage,

        @Schema(example = "Formal", defaultValue = "Formal")
        String style,

        @Schema(example = "OPENAI")
        AiProvider provider

) implements AiPayload {}