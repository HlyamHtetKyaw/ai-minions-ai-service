package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.request.ContentImageV2Request;
import com.aiminion.aiservice.feature.request.ContentTextRequest;
import com.aiminion.aiservice.feature.request.ContentV2Request;
import com.aiminion.aiservice.feature.response.ContentImageV2Response;
import com.aiminion.aiservice.feature.response.ContentTextResponse;
import com.aiminion.aiservice.feature.response.ContentV2Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentV2AiService
        implements BaseAiServiceGenerator<ContentV2Request, ContentV2Response>,
        AiFeatureHandler {
    private static final String DEFAULT_LANG = "English";
    private static final String TEXT_LENGTH_LONG = "LONG";

    private final ContentTextAiService contentTextAiService;
    private final ContentImageV2AiService contentImageV2AiService;

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_V2;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentV2Request req = objectMapper.convertValue(request.payload(), ContentV2Request.class);
        ContentV2Response result = generate(req);
        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_V2)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentV2Response generate(ContentV2Request req) {
        boolean longText = TEXT_LENGTH_LONG.equalsIgnoreCase(req.textLength());
        String contentType = longText ? "BLOG" : req.contentType();
        String style = normalizeStyleByTextLength(req.style(), longText);

        ContentTextRequest textReq = ContentTextRequest.builder()
                .topic(req.topic())
                .contentType(contentType)
                .sourceLanguage(resolveLanguage(req.sourceLanguage()))
                .targetLanguage(resolveLanguage(req.targetLanguage()))
                .style(style)
                .provider(AiProvider.GEMINI)
                .build();
        ContentTextResponse text = contentTextAiService.generate(textReq);

        String imagePrompt = firstNonBlank(text.title(), req.topic(), text.generatedContent());
        ContentImageV2Request imageReq = ContentImageV2Request.builder()
                .prompt(imagePrompt)
                .size(req.imageSize())
                .shortText(req.shortText())
                .aiOverlayTextEnabled(req.aiOverlayTextEnabled())
                .userOverlayText(req.userOverlayText())
                .toonStyle(req.toonStyle())
                .logoUrl(req.logoUrl())
                .photoUrl(req.photoUrl())
                .provider(AiProvider.GEMINI)
                .logoPosition(req.logoPosition())
                .logoWidth(req.logoWidth())
                .logoHeight(req.logoHeight())
                .logoMargin(req.logoMargin())
                .photoPosition(req.photoPosition())
                .photoWidth(req.photoWidth())
                .photoHeight(req.photoHeight())
                .photoMargin(req.photoMargin())
                .build();
        ContentImageV2Response image = contentImageV2AiService.generate(imageReq);
        ContentTextResponse syncedText = ContentTextResponse.builder()
                .title(image.shortText())
                .contentType(text.contentType())
                .generatedFrom(text.generatedFrom())
                .generatedTo(text.generatedTo())
                .style(text.style())
                .usedProvider(text.usedProvider())
                .generatedContent(text.generatedContent())
                .build();

        return ContentV2Response.builder()
                .text(syncedText)
                .image(image)
                .usedProvider(AiProvider.GEMINI)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "AI Generated";
    }

    private String resolveLanguage(String lang) {
        return isBlank(lang) ? DEFAULT_LANG : lang.trim();
    }

    private String normalizeStyleByTextLength(String style, boolean longText) {
        String base = isBlank(style) ? "Professional" : style.trim();
        if (!longText) {
            return base + " | concise output (1-2 lines)";
        }
        return base + " | detailed long-form output";
    }
}
