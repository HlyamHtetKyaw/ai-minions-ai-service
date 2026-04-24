package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.prompt.impl.PromptBuilderImpl;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.sanitizer.service.SanitizerService;
import com.aiminion.aiservice.common.util.AIStyles;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.request.TranslateRequest;
import com.aiminion.aiservice.feature.response.TranslateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateAiService implements BaseAiServiceGenerator<TranslateRequest, TranslateResponse>, AiFeatureHandler {

    private final AiContentTextGenerator aiContentTextGenerator;
    private final PromptBuilderImpl promptBuilderImpl;
    private final SanitizerService sanitizerService;

    private static final String DEFAULT_SOURCE = "English";
    private static final String DEFAULT_TARGET = "Myanmar";
    private static final String DEFAULT_STYLE  = "Formal";

    //AiFeatureHandler

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.TRANSLATE;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        TranslateRequest translateRequest = objectMapper.convertValue(
                request.payload(), TranslateRequest.class
        );

        if (request.provider() != null) {
            translateRequest = TranslateRequest.builder()
                    .text(translateRequest.text())
                    .style(translateRequest.style())
                    .provider(request.provider())
                    .build();
        }

        // Sanitize Method - checks for harmful content and throws if unsafe
        sanitizerService.sanitize(translateRequest.text(), FeatureType.TRANSLATE , request.provider());

        boolean getStyles = request.payload().path("getStyles").asBoolean(false);
        if(getStyles){
            TranslateResponse result = getTranslateStyles(translateRequest);

            return AiGenerateResponse.builder()
                    .featureType(FeatureType.TRANSLATE)
                    .usedProvider(result.usedProvider())
                    .result(result)
                    .build();
        }

        TranslateResponse result = generate(translateRequest);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.TRANSLATE)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    private TranslateResponse getTranslateStyles(TranslateRequest translateRequest) {
        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(promptBuilderImpl.buildTranslateStylePrompt())
                .userMessage(translateRequest.text())
                .provider(translateRequest.provider())
                .build();

        List<String> aiLatestModels = aiContentTextGenerator.getAiLatestModels(aiRequest);

        return TranslateResponse.builder()
                .aiLatestModels(aiLatestModels)
                .styles(AIStyles.getTranslateStyles())
                .build();
    }

    @Override
    public TranslateResponse generate(TranslateRequest req) {
        String source = isBlank(req.sourceLanguage()) ? DEFAULT_SOURCE : req.sourceLanguage().trim();
        String target = isBlank(req.targetLanguage()) ? DEFAULT_TARGET : req.targetLanguage().trim();
        String style  = isBlank(req.style())          ? DEFAULT_STYLE  : req.style().trim();

        log.info("[Translate] {}→{} style={}", source, target, style);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(promptBuilderImpl.buildTranslatePrompt(source, target, style))
                .userMessage(req.text())
                .provider(req.provider())
                .build();

        AiResponse aiResponse = aiContentTextGenerator.generate(aiRequest);

        return TranslateResponse.builder()
                .translatedText(aiResponse.content())
                .translatedFrom(source)
                .translatedTo(target)
                .style(style)
                .usedProvider(aiResponse.usedProvider())
                .tokenIn(aiResponse.tokenIn())
                .tokenOut(aiResponse.tokenOut())
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}