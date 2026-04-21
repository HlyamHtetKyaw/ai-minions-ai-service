package com.aiminion.aiservice.common.ai.handler;

import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface InlineAudioFeatureHandler {
	AiGenerateResponse handleInlineAudio(
			AiGenerateRequest request,
			byte[] audioBytes,
			String filename,
			String mimeType,
			ObjectMapper objectMapper);
}

