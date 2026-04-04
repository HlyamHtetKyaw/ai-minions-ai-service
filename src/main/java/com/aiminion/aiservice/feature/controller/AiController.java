package com.aiminion.aiservice.feature.controller;

import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.router.AiServiceRouter;
import com.aiminion.aiservice.common.response.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
            description = "Routes to the correct AI feature based on featureType."
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(
            @Valid @RequestBody AiGenerateRequest request) {

        final AiGenerateResponse response = aiServiceRouter.route(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Generated successfully."));
    }
}