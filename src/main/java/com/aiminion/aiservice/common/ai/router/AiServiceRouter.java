package com.aiminion.aiservice.common.ai.router;

import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.audio.AudioTranscribeFeatureHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceRouter {

    private final List<AiFeatureHandler> handlers;
    private final ObjectMapper objectMapper;

    private Map<FeatureType, AiFeatureHandler> handlerMap;

    @PostConstruct
    public void init() {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(AiFeatureHandler::getFeatureType, Function.identity()));
        log.info("AiServiceRouter registered features: {}", handlerMap.keySet());
    }

    public AiGenerateResponse route(AiGenerateRequest request) {
        AiFeatureHandler handler = handlerMap.get(request.featureType());

        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported feature type: " + request.featureType()
            );
        }

        log.info("[AiServiceRouter] Routing to feature={} provider={}",
                request.featureType(), request.provider());

        return handler.handle(request, objectMapper);
    }
	public AiGenerateResponse routeWithInlineAudio(
			AiGenerateRequest request,
			byte[] audioBytes,
			String filename,
			String mimeType) {
		if (request.featureType() != FeatureType.TRANSCRIBE) {
			throw new IllegalArgumentException("Inline audio is only supported for featureType TRANSCRIBE");
		}
		AiFeatureHandler handler = handlerMap.get(FeatureType.TRANSCRIBE);
		if (!(handler instanceof AudioTranscribeFeatureHandler audioHandler)) {
			throw new IllegalStateException("TRANSCRIBE handler is not AudioTranscribeFeatureHandler");
		}
		log.info("[AiServiceRouter] Inline TRANSCRIBE provider={} ({} bytes)", request.provider(), audioBytes.length);
		return audioHandler.handleInlineAudio(request, audioBytes, filename, mimeType, objectMapper);
	}
}