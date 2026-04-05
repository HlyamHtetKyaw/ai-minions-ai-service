package com.aiminion.aiservice.feature.request;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.swagger.AiPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(
        name = "TranslatePayload",
        description = "Payload for translation feature"
)
@Builder
public record TranslateRequest(

        @Schema(description = "Text to translate", example = "Hello, how are you?")
        @NotBlank(message = "Text must not be blank.")
        @Size(max = 1500, message = "Text must not exceed 1500 characters.")
        String text,

        @Schema(example = "English", defaultValue = "English")
        String sourceLanguage,

        @Schema(example = "Myanmar", defaultValue = "Myanmar")
        String targetLanguage,

        @Schema(example = "Formal", defaultValue = "Formal")
        String style,

        @Schema(example = "OPENAI")
        AiProvider provider

) implements AiPayload {}