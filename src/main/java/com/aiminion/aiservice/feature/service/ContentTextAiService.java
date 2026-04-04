package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.prompt.impl.PromptBuilderImpl;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.request.ContentTextRequest;
import com.aiminion.aiservice.feature.response.ContentTextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTextAiService
        implements BaseAiServiceGenerator<ContentTextRequest, ContentTextResponse>,
        AiFeatureHandler {

    private final AiContentTextGenerator aiContentTextGenerator;
    private final PromptBuilderImpl promptBuilderImpl;

    private static final String DEFAULT_SOURCE = "English";
    private static final String DEFAULT_TARGET = "Myanmar";
    private static final String DEFAULT_STYLE  = "Formal";

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_TEXT;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentTextRequest req = objectMapper.convertValue(request.payload(), ContentTextRequest.class);

        if (request.provider() != null) {
            req = ContentTextRequest.builder()
                    .topic(req.topic())
                    .sourceLanguage(req.sourceLanguage())
                    .targetLanguage(req.targetLanguage())
                    .style(req.style())
                    .provider(request.provider())
                    .build();
        }

        ContentTextResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_TEXT)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentTextResponse generate(ContentTextRequest req) {
        String source = isBlank(req.sourceLanguage()) ? DEFAULT_SOURCE : req.sourceLanguage().trim();
        String target = isBlank(req.targetLanguage()) ? DEFAULT_TARGET : req.targetLanguage().trim();
        String style  = isBlank(req.style())          ? DEFAULT_STYLE  : req.style().trim();

        log.info("[ContentText] {}→{} style={}", source, target, style);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(promptBuilderImpl.buildContentTextPrompt(source, target, style))
                .userMessage(req.topic())
                .provider(req.provider())
                .build();

        AiResponse aiResponse = aiContentTextGenerator.generate(aiRequest);

        return ContentTextResponse.builder()
                .generatedContent(aiResponse.content())
                .generatedFrom(source)
                .generatedTo(target)
                .style(style)
                .usedProvider(aiResponse.usedProvider())
                .build();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}