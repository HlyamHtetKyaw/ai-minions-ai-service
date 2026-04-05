package com.aiminion.aiservice.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.router.AiServiceRouter;
import com.aiminion.aiservice.common.response.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/${api.base.path}/feature")
@RequiredArgsConstructor
@Tag(name = "AI Service", description = "Single entry point for all AI features.")
public class AiController {

	private final AiServiceRouter aiServiceRouter;

	@Operation(
			summary = "Generate AI Content",
			description = "Routes by featureType. AUDIO/transcribe is not supported here — use multipart /generate with an `audio` part."
	)
	@PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(@Valid @RequestBody AiGenerateRequest request) {
		AiGenerateResponse response = aiServiceRouter.route(request);
		return ResponseEntity.ok(ApiResponse.success(response, "Generated successfully."));
	}

	@Operation(
			summary = "Generate with raw audio (transcribe)",
			description = "Multipart: JSON `request` part (Content-Type: application/json; same shape as /generate) + binary `audio` part."
	)
	@PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<AiGenerateResponse>> generateMultipart(
			@RequestPart("request") @Valid AiGenerateRequest request,
			@RequestPart("audio") MultipartFile audio) {
		if (audio == null || audio.isEmpty()) {
			throw new IllegalArgumentException("audio part is required and must not be empty");
		}
		byte[] audioBytes;
		try {
			audioBytes = audio.getBytes();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read audio part", e);
		}
		AiGenerateResponse response = aiServiceRouter.routeWithInlineAudio(
				request,
				audioBytes,
				audio.getOriginalFilename(),
				audio.getContentType());
		return ResponseEntity.ok(ApiResponse.success(response, "Generated successfully."));
	}
}
