package com.aiminion.aiservice.feature.service;
import com.aiminion.aiservice.common.ai.generator.AiContentImageGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;

import com.aiminion.aiservice.feature.imageOverlay.request.OverlayRequest;
import com.aiminion.aiservice.feature.imageOverlay.response.OverlayResult;
import com.aiminion.aiservice.feature.imageOverlay.service.ImageOverlayService;
import com.aiminion.aiservice.feature.request.ContentImageRequest;
import com.aiminion.aiservice.feature.response.ContentImageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentImageAiService
        implements BaseAiServiceGenerator<ContentImageRequest, ContentImageResponse>,
        AiFeatureHandler {

    private final AiContentImageGenerator aiContentImageGenerator;
    private final ImageOverlayService imageOverlayService;

    private static final String DEFAULT_SIZE    = "1024x1024";
    private static final String DEFAULT_QUALITY = "standard";

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_IMAGE;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentImageRequest req = objectMapper.convertValue(request.payload(), ContentImageRequest.class);

        if (request.provider() != null) {
            req = ContentImageRequest.builder()
                    .prompt(req.prompt())
                    .size(req.size())
                    .quality(req.quality())
                    .provider(request.provider())
                    .build();
        }

        ContentImageResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_IMAGE)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentImageResponse generate(ContentImageRequest req) {
        String size    = isBlank(req.size())    ? DEFAULT_SIZE    : req.size().trim();
        String quality = isBlank(req.quality()) ? DEFAULT_QUALITY : req.quality().trim();

        log.info("[ContentImage] Generating image — size={} quality={}", size, quality);

        String imageUrl  = aiContentImageGenerator.generateImage(req.prompt(), size, quality);
        boolean hasOverlay = !isBlank(req.logoUrl()) || !isBlank(req.photoUrl());

        if (hasOverlay) {
            OverlayRequest overlayRequest = OverlayRequest.builder()
                    .baseImageUrl(imageUrl)
                    .logoUrl(req.logoUrl())
                    .photoUrl(req.photoUrl())
                    .build();

            OverlayResult overlayResult = imageOverlayService.compose(overlayRequest);

            return ContentImageResponse.builder()
                    .imageBytes(overlayResult.imageBytes())   // ← composed image
                    .imageName(overlayResult.imageName())
                    .prompt(req.prompt())
                    .usedProvider(AiProvider.OPENAI)
                    .build();
        }

        return ContentImageResponse.builder()
                .imageUrl(imageUrl)
                .imageName(aiContentImageGenerator.generateImageName())
                .prompt(req.prompt())
                .usedProvider(AiProvider.OPENAI)   // DALL-E is OpenAI only
                .build();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}