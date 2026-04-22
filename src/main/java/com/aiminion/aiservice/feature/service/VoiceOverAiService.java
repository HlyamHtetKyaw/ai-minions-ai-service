package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiVoiceOverGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.util.MediaFileNameGenerator;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.integration.ProcessingStorageClient;
import com.aiminion.aiservice.feature.request.VoiceOverRequest;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceOverAiService implements BaseAiServiceGenerator<VoiceOverRequest, VoiceOverResponse>, AiFeatureHandler {

    private final AiVoiceOverGenerator aiVoiceOverGenerator;
    private final MediaFileNameGenerator mediaFileNameGenerator;
    private final ProcessingStorageClient processingStorageClient;

    private static final String DEFAULT_SOURCE = "English";
    private static final String DEFAULT_TARGET = "Myanmar";
    private static final String DEFAULT_STYLE  = "Formal";
    private static final String DEFAULT_AI_MODEL = "Alex";
    private static final String DEFAULT_TEXT_LENGTH = "Short";

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.VOICEOVER;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        VoiceOverRequest req = objectMapper.convertValue(request.payload(), VoiceOverRequest.class);

        if (request.provider() != null) {
            req = VoiceOverRequest.builder()
                    .text(req.text())
                    .aiModel(req.aiModel())
                    .sourceLanguage(req.sourceLanguage())
                    .targetLanguage(req.targetLanguage())
                    .style(req.style())
                    .textLength(req.textLength())
                    .provider(request.provider())
                    .build();
        }

        VoiceOverResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.VOICEOVER)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public VoiceOverResponse generate(VoiceOverRequest req) {
        String source     = isBlank(req.sourceLanguage()) ? DEFAULT_SOURCE      : req.sourceLanguage().trim();
        String target     = isBlank(req.targetLanguage()) ? DEFAULT_TARGET      : req.targetLanguage().trim();
        String style      = isBlank(req.style())          ? DEFAULT_STYLE       : req.style().trim();
        String aiModel    = isBlank(req.aiModel())        ? DEFAULT_AI_MODEL    : req.aiModel().trim();
        String textLength = isBlank(req.textLength())     ? DEFAULT_TEXT_LENGTH : req.textLength().trim();

        log.info("[VoiceOver] source={} target={} style={} voice={} textLength={} provider={}",
                source, target, style, aiModel, textLength, req.provider());

        double speed = resolveSpeed(textLength);

        // provider=null → falls back to ai.tts-default-provider in AiVoiceOverGenerator
        VoiceOverResponse response = aiVoiceOverGenerator.generateAudio(req.provider(), req.text(), aiModel, speed);

        String fileName = mediaFileNameGenerator.generate(
                req.username(), req.userId(), "AiVoiceOver", "mp3"
        );

        ProcessingStorageClient.StoredAudio stored = processingStorageClient.storeAudioLocally(
                response.audioByte(),
                fileName,
                "audio/mpeg"
        );

//        ProcessingStorageClient.StoredAudio stored = processingStorageClient.storeAudio(
//                response.audioByte(),
//                "voice-over/" + fileName,
//                "audio/mpeg"
//        );

        log.info("[VoiceOver] Audio stored → url={} key={}", stored.storageUrl(), stored.key());

        return VoiceOverResponse.builder()
                .audioUrl(stored.storageUrl())
                .audioByte(response.audioByte())
                .audioBase64(response.audioBase64())
                .sourceLanguage(source)
                .targetLanguage(target)
                .style(style)
                .rawOutput(req.text())
                .usedProvider(req.provider() != null ? req.provider() : AiProvider.GEMINI)
                .tokenIn(response.tokenIn())
                .tokenOut(response.tokenOut())
                .build();
    }

    /**
     * Slows down LONG scripts slightly for natural pacing.
     */
    private double resolveSpeed(String textLength) {
        return switch (textLength.toUpperCase()) {
            case "LONG"   -> 0.9;
            case "MEDIUM" -> 0.95;
            default       -> 1.0;   // SHORT
        };
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

}
