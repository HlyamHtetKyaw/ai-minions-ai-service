package com.aiminion.aiservice.feature.service;
import com.aiminion.aiservice.common.ai.generator.AiContentImageGenerator;
import com.aiminion.aiservice.common.ai.generator.GeminiImagenImageGenerator;
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

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentImageAiService
        implements BaseAiServiceGenerator<ContentImageRequest, ContentImageResponse>,
        AiFeatureHandler {

    private final AiContentImageGenerator aiContentImageGenerator;
    private final GeminiImagenImageGenerator geminiImagenImageGenerator;

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
//                    .logoUrl(req.logoUrl())
//                    .photoUrl(req.photoUrl())
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

        AiProvider provider = req.provider() != null ? req.provider() : AiProvider.OPENAI;
        log.info("[ContentImage] Generating image — provider={} size={} quality={}", provider, size, quality);

        String imageUrl;
        String imageName = aiContentImageGenerator.generateImageName();
        if (provider == AiProvider.GEMINI) {
            String aspectRatio = mapSizeToAspectRatio(size);
            String imageSize = mapQualityToImageSize(quality);
            imageUrl = geminiImagenImageGenerator.generateImageDataUrl(req.prompt(), aspectRatio, imageSize);
        } else {
            imageUrl = aiContentImageGenerator.generateImageWithOpenAi(req.prompt(), size, quality);
        }

        return ContentImageResponse.builder()
                .imageUrl(imageUrl)
                .imageName(aiContentImageGenerator.generateImageName())
                .prompt(req.prompt())
                .usedProvider(provider)
                .build();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String mapSizeToAspectRatio(String size) {
        // OpenAI sizes are WxH. Imagen expects aspect ratios like "1:1", "16:9", etc.
        if (size == null) return "1:1";
        String s = size.trim().toLowerCase();
        return switch (s) {
            case "1024x1024", "512x512" -> "1:1";
            case "1024x1792", "768x1344" -> "9:16";
            case "1792x1024", "1344x768" -> "16:9";
            default -> "1:1";
        };
    }

    private static String mapQualityToImageSize(String quality) {
        // Imagen API commonly uses "1K" or "2K".
        if (quality == null) return "1K";
        String q = quality.trim().toLowerCase();
        return "hd".equals(q) ? "2K" : "1K";
    }
}