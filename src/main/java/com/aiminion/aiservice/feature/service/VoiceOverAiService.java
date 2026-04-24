package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiVoiceOverGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.util.AIStyles;
import com.aiminion.aiservice.common.util.MediaFileNameGenerator;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.integration.ProcessingStorageClient;
import com.aiminion.aiservice.feature.request.VoiceOverRequest;
import com.aiminion.aiservice.feature.response.VoiceModelDescriptor;
import com.aiminion.aiservice.feature.response.VoiceOverModelsResponse;
import com.aiminion.aiservice.feature.response.VoiceOverProviderModels;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.singletonList;

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

        boolean listModels = request.payload().path("getVoiceModels").asBoolean(false)
                || request.payload().path("getStyles").asBoolean(false);
        if (listModels) {
            AiProvider p = req.provider() != null ? req.provider() : AiProvider.GEMINI;
            List<VoiceModelDescriptor> models = aiVoiceOverGenerator.listVoiceModels(p);
            VoiceOverModelsResponse result = VoiceOverModelsResponse.builder()
                    .providers(singletonList(VoiceOverProviderModels.builder()
                            .provider(p.name())
                            .models(models)
                            .build()))
                    .build();

            return AiGenerateResponse.builder()
                    .featureType(FeatureType.VOICEOVER)
                    .usedProvider(p)
                    .result(result)
                    .build();
        }

        VoiceOverResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.VOICEOVER)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

//=======
//    private VoiceOverResponse getAiVoiceOverData(VoiceOverRequest req) {
//        AiRequest aiRequest = AiRequest.builder()
//                .systemPrompt(promptBuilderImpl.buildVoiceOverDataPrompt())
//                .userMessage("List the latest available Gemini TTS voice models.")
//                .provider(req.provider())
//                .build();
//
//        List<String> aiLatestModels = aiVoiceOverGenerator.generate(aiRequest);
//
//        return VoiceOverResponse.builder()
//                .aiLatestModels(aiLatestModels)
//                .styles(AIStyles.voiceOverStyles())
//                .build();
//    }
//
//>>>>>>> 1ac1bb318ba4de2855c3c95fbcb50edcad2afbaf
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

        // to Test in Local
//        ProcessingStorageClient.StoredAudio stored = processingStorageClient.storeAudioLocally(
//                response.audioByte(),
//                fileName,
//                "audio/mpeg"
//        );

        ProcessingStorageClient.StoredAudio stored = processingStorageClient.storeAudio(
                response.audioByte(),
                "voice-over/" + fileName,
                "audio/mpeg"
        );

        log.info("[VoiceOver] Audio stored → url={} key={}", stored.storageUrl(), stored.key());

        return VoiceOverResponse.builder()
                .audioUrl(stored.storageUrl())
                .s3Key(stored.key())
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
