package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.imageOverlay.service.ImageOverlayService;
import com.aiminion.aiservice.feature.request.ContentImageRequest;
import com.aiminion.aiservice.feature.request.ContentRequest;
import com.aiminion.aiservice.feature.request.ContentTextRequest;
import com.aiminion.aiservice.feature.response.ContentImageResponse;
import com.aiminion.aiservice.feature.response.ContentResponse;
import com.aiminion.aiservice.feature.response.ContentTextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAiService
        implements BaseAiServiceGenerator<ContentRequest, ContentResponse>,
        AiFeatureHandler {

    // Delegates to the two focused services — no duplicated logic
    private final ContentTextAiService  contentTextAiService;
    private final ContentImageAiService contentImageAiService;
    private final ImageOverlayService imageOverlayService;

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentRequest req = objectMapper.convertValue(request.payload(), ContentRequest.class);

        if (request.provider() != null) {
            req = ContentRequest.builder()
                    .topic(req.topic())
//                    .contentType(req.contentType())
                    .sourceLanguage(req.sourceLanguage())
                    .targetLanguage(req.targetLanguage())
                    .style(req.style())
                    .imageSize(req.imageSize())
                    .imageQuality(req.imageQuality())
//                    .logoUrl(req.logoUrl())
//                    .photoUrl(req.photoUrl())
//                    .provider(request.provider())
//
//                    .logoPosition(req.logoPosition())
//                    .logoWidth(req.logoWidth())
//                    .logoHeight(req.logoHeight())
//                    .logoMargin(req.logoMargin())
//
//                    .photoPosition(req.photoPosition())
//                    .photoUrl(req.photoUrl())
//                    .photoWidth(req.photoWidth())
//                    .photoHeight(req.photoHeight())
                    .build();
        }

        ContentResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentResponse generate(ContentRequest req) {
        log.info("[Content] Generating text + image for topic='{}'", req.topic());

        ContentTextRequest textRequest = ContentTextRequest.builder()
                .topic(req.topic())
//                .contentType(req.contentType())
                .sourceLanguage(req.sourceLanguage())
                .targetLanguage(req.targetLanguage())
                .style(req.style())
                .textLength(null)
                .provider(req.provider())
                .build();

        ContentTextResponse text = contentTextAiService.generate(textRequest);

        log.info("[Content] Using AI title as image prompt='{}'", text.title());

        System.out.println("Image Title to Generate : " + text.title());

        ContentImageRequest imageRequest = ContentImageRequest.builder()
                .prompt(text.title())
                .size(req.imageSize())
                .quality(req.imageQuality())
                .provider(req.provider())
//                .logoUrl(req.logoUrl())
//                .photoUrl(req.photoUrl())
//
//                .logoPosition(req.logoPosition())
//                .logoWidth(req.logoWidth())
//                .logoHeight(req.logoHeight())
//                .logoMargin(req.logoMargin())
//
//                .photoPosition(req.photoPosition())
//                .photoUrl(req.photoUrl())
//                .photoWidth(req.photoWidth())
//                .photoHeight(req.photoHeight())
                .build();

        ContentImageResponse image = contentImageAiService.generate(imageRequest);

        return ContentResponse.builder()
                .text(text)
                .image(image)
                .usedProvider(text.usedProvider())
                .imageBytes(image.imageBytes())
                .build();
    }
}