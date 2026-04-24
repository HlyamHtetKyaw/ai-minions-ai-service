package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.sanitizer.service.SanitizerService;
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
    private final SanitizerService sanitizerService;

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_V2;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentV2Request req = objectMapper.convertValue(request.payload(), ContentV2Request.class);

        // Sanitizer for GENERATE_CONTENT_V2
        sanitizerService.sanitize(req.topic(), FeatureType.GENERATE_CONTENT_V2 , request.provider());

        ContentV2Response result = generate(req);
        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_V2)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentV2Response generate(ContentV2Request req) {
        if (req.outputMode() != null && "textOnly".equalsIgnoreCase(req.outputMode().trim())) {
            boolean longText = TEXT_LENGTH_LONG.equalsIgnoreCase(req.textLength());
            String style = normalizeStyleByTextLength(req.style(), longText);

            ContentTextRequest textReq = ContentTextRequest.builder()
                    .topic(req.topic())
                    .contentType(req.contentType())
                    .sourceLanguage(resolveLanguage(req.sourceLanguage()))
                    .targetLanguage(resolveLanguage(req.targetLanguage()))
                    .style(style)
                    .textLength(req.textLength())
                    .provider(AiProvider.GEMINI)
                    .build();
            ContentTextResponse text = contentTextAiService.generate(textReq);

            return ContentV2Response.builder()
                    .text(text)
                    .image(null)
                    .usedProvider(AiProvider.GEMINI)
                    .build();
        }

        boolean longText = TEXT_LENGTH_LONG.equalsIgnoreCase(req.textLength());
        String style = normalizeStyleByTextLength(req.style(), longText);

        ContentTextRequest textReq = ContentTextRequest.builder()
                .topic(req.topic())
                .contentType(req.contentType())
                .sourceLanguage(resolveLanguage(req.sourceLanguage()))
                .targetLanguage(resolveLanguage(req.targetLanguage()))
                .style(style)
                .textLength(req.textLength())
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
                .contentType(req.contentType())
                .tone(req.style())
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
            return base + " | keep length tight for the selected content type.";
        }
        return base + " | go deeper per the content-type instructions (stay on-format; do not switch to an unrelated genre).";
    }
}
