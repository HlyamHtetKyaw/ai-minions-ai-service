package com.aiminion.aiservice.feature.controller;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/${api.base.path}/feature")
@RequiredArgsConstructor
public class AiController {

	private final AiServiceRouter aiServiceRouter;

	@PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(@Valid @RequestBody AiGenerateRequest request) {
		AiGenerateResponse response = aiServiceRouter.route(request);
		return ResponseEntity.ok(ApiResponse.success(response, "Generated successfully."));
	}

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
