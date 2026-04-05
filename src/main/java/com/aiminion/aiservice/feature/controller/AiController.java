package com.aiminion.aiservice.feature.controller;

import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.router.AiServiceRouter;
import com.aiminion.aiservice.common.response.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.base.path}/feature")
@RequiredArgsConstructor
@Tag(name = "AI Service", description = "Single entry point for all AI features.")
public class AiController {

    private final AiServiceRouter aiServiceRouter;

    @Operation(
            summary = "Generate AI Content",
            description = "Routes to the correct AI feature based on featureType.\n\n" +
                    "Supported feature types:\n" +
                    "- TRANSLATE\n" +
                    "- GENERATE_CONTENT_TEXT\n" +
                    "- GENERATE_CONTENT_IMAGE\n" +
                    "- GENERATE_CONTENT"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully generated content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "AI Generate Request with dynamic payload based on featureType",
                    content = @Content(
                            schema = @Schema(implementation = AiGenerateRequest.class),
                            examples = {

                                    @ExampleObject(
                                            name = "Translate",
                                            summary = "Translate text",
                                            value = """
                                        {
                                          "featureType": "TRANSLATE",
                                          "provider": "OPENAI",
                                          "payload": {
                                            "text": "Hello, how are you today?",
                                            "sourceLanguage": "English",
                                            "targetLanguage": "Myanmar",
                                            "style": "Formal"
                                          }
                                        }
                                        """
                                    ),

                                    @ExampleObject(
                                            name = "Generate Text",
                                            summary = "Generate text content",
                                            value = """
                                        {
                                          "featureType": "GENERATE_CONTENT_TEXT",
                                          "provider": "OPENAI",
                                          "payload": {
                                            "topic": "Importance of sleep",
                                            "sourceLanguage": "English",
                                            "targetLanguage": "Myanmar",
                                            "style": "Formal"
                                          }
                                        }
                                        """
                                    ),

                                    @ExampleObject(
                                            name = "Generate Image",
                                            summary = "Generate image",
                                            value = """
                                        {
                                          "featureType": "GENERATE_CONTENT_IMAGE",
                                          "provider": "OPENAI",
                                          "payload": {
                                            "prompt": "A peaceful Myanmar village at sunset",
                                            "size": "1024x1024",
                                            "quality": "standard"
                                          }
                                        }
                                        """
                                    ),

                                    @ExampleObject(
                                            name = "Generate Text + Image",
                                            summary = "Generate both text and image",
                                            value = """
                                        {
                                          "featureType": "GENERATE_CONTENT",
                                          "provider": "OPENAI",
                                          "payload": {
                                            "topic": "Importance of sleep",
                                            "sourceLanguage": "English",
                                            "targetLanguage": "Myanmar",
                                            "style": "Formal",
                                            "imageSize": "1024x1024",
                                            "imageQuality": "standard"
                                          }
                                        }
                                        """
                                    )
                            }
                    )
            )
            @Valid @RequestBody AiGenerateRequest request) {

        final AiGenerateResponse response = aiServiceRouter.route(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Generated successfully."));
    }
}