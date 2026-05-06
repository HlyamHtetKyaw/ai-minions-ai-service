package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.generator.GeminiImageGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.integration.ProcessingStorageClient;
import com.aiminion.aiservice.feature.imageOverlay.request.OverlayRequest;
import com.aiminion.aiservice.feature.imageOverlay.response.OverlayResult;
import com.aiminion.aiservice.feature.imageOverlay.service.ImageOverlayService;
import com.aiminion.aiservice.feature.request.ContentImageV2Request;
import com.aiminion.aiservice.feature.response.ContentImageV2Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentImageV2AiService
        implements BaseAiServiceGenerator<ContentImageV2Request, ContentImageV2Response>,
        AiFeatureHandler {

    private static final String DEFAULT_SIZE = "1024x1024";
    private static final Pattern MYANMAR_CHAR_PATTERN = Pattern.compile("[\\u1000-\\u109F]");

    private final GeminiImageGenerator geminiImageGenerator;
    private final ImageOverlayService imageOverlayService;
    private final AiContentTextGenerator aiContentTextGenerator;
    private final ProcessingStorageClient processingStorageClient;

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_IMAGE_V2;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentImageV2Request req = objectMapper.convertValue(request.payload(), ContentImageV2Request.class);

        ContentImageV2Response result = generate(req);
        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_IMAGE_V2)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentImageV2Response generate(ContentImageV2Request req) {
        String size = isBlank(req.size()) ? DEFAULT_SIZE : req.size().trim();
        boolean aiOverlayEnabled = Boolean.TRUE.equals(req.aiOverlayTextEnabled());
        String rawShortText = isBlank(req.shortText()) ? autoShortText(req.prompt()) : req.shortText().trim();
        String aiOverlayText = aiOverlayEnabled ? ensureMyanmarOverlayText(rawShortText) : "";
        String userOverlayText = isBlank(req.userOverlayText()) ? "" : sanitizeCaption(req.userOverlayText());
        String imageScenePrompt = buildSyncedImageScenePrompt(
                req.prompt(),
                firstNonBlank(userOverlayText, aiOverlayText, req.prompt())
        );

        GeminiImageGenerator.GeneratedImage generated = geminiImageGenerator.generateToonImage(
                imageScenePrompt,
                size,
                req.toonStyle(),
                firstNonBlank(aiOverlayText, userOverlayText, req.prompt()),
                req.logoUrl(),
                req.photoUrl(),
                req.contentType(),
                req.tone()
        );

        byte[] imageBytes;
        String imageName;
        boolean needsOverlay = !isBlank(req.logoUrl())
                || !isBlank(req.photoUrl())
                || !aiOverlayText.isBlank()
                || !userOverlayText.isBlank();
        if (needsOverlay) {
            // Deterministic post-compose so logo/photo/text always appear when provided.
            OverlayRequest overlayRequest = OverlayRequest.builder()
                    .baseImageUrl(toDataUri(generated.mimeType(), generated.bytes()))
                    .logoUrl(req.logoUrl())
                    .photoUrl(req.photoUrl())
                    .aiShortText(aiOverlayText)
                    .userShortText(userOverlayText)
                    .textPosition("BOTTOM")
                    .logoPosition(req.logoPosition())
                    .logoWidth(req.logoWidth())
                    .logoHeight(req.logoHeight())
                    .logoMargin(req.logoMargin())
                    .photoPosition(req.photoPosition())
                    .photoWidth(req.photoWidth())
                    .photoHeight(req.photoHeight())
                    .photoMargin(req.photoMargin())
                    .build();
            OverlayResult composed = imageOverlayService.compose(overlayRequest);
            imageBytes = composed.imageBytes();
            imageName = composed.imageName();
        } else {
            imageBytes = generated.bytes();
            imageName = generated.imageName();
        }

        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        String keyHint = "content-generator/v2/generated-" + Instant.now().toEpochMilli() + ".png";
        ProcessingStorageClient.StoredImage storedImage = processingStorageClient.storeImage(imageBytes, keyHint);

        return ContentImageV2Response.builder()
                .imageName(imageName)
                .prompt(req.prompt())
                .shortText(joinOverlaySummary(aiOverlayText, userOverlayText))
                .imageBytes(imageBytes)
                .imageBase64(imageBase64)
                .storageUrl(storedImage.storageUrl())
                .s3Key(storedImage.key())
                .usedProvider(AiProvider.GEMINI)
                .build();
    }

    private String autoShortText(String prompt) {
        AiRequest aiRequest = AiRequest.builder()
                .provider(AiProvider.GEMINI)
                .systemPrompt("""
                        You write short social-media image captions.
                        Return exactly one line in clear, natural Burmese (Myanmar Unicode).
                        It must be easy to understand at first read.
                        Keep it concise (max 8 words).
                        No hashtags, no emojis, no markdown, no quotes, no labels.
                        """)
                .userMessage(prompt)
                .build();
        try {
            AiResponse response = aiContentTextGenerator.generate(aiRequest);
            String text = response.content() == null ? "" : response.content().trim();
            return text.isBlank() ? "AI Generated" : text;
        } catch (Exception ex) {
            log.warn("[ContentImageV2] Failed to auto-generate short text, using fallback: {}", ex.getMessage());
            return "AI Generated";
        }
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
        return "";
    }

    private String joinOverlaySummary(String aiText, String userText) {
        if (!isBlank(aiText) && !isBlank(userText)) {
            return aiText + " | " + userText;
        }
        if (!isBlank(userText)) {
            return userText;
        }
        if (!isBlank(aiText)) {
            return aiText;
        }
        return "";
    }

    private String ensureMyanmarOverlayText(String text) {
        if (isBlank(text)) {
            return "AI ဖန်တီးမှု";
        }
        if (MYANMAR_CHAR_PATTERN.matcher(text).find()) {
            return normalizeMyanmarCaption(sanitizeCaption(text));
        }
        String translated = translateToMyanmar(text, false);
        if (MYANMAR_CHAR_PATTERN.matcher(translated).find()) {
            return normalizeMyanmarCaption(sanitizeCaption(translated));
        }
        // Retry once with stricter constraints if first translation comes back weak.
        String strictTranslated = translateToMyanmar(text, true);
        if (MYANMAR_CHAR_PATTERN.matcher(strictTranslated).find()) {
            return normalizeMyanmarCaption(sanitizeCaption(strictTranslated));
        }
        return sanitizeCaption(text);
    }

    private String normalizeScenePromptForImage(String topicPrompt) {
        if (isBlank(topicPrompt)) {
            return "A child studying from a Myanmar language textbook in a room, toon style.";
        }
        if (!MYANMAR_CHAR_PATTERN.matcher(topicPrompt).find()) {
            return topicPrompt;
        }
        AiRequest aiRequest = AiRequest.builder()
                .provider(AiProvider.GEMINI)
                .systemPrompt("""
                        Convert the input into a concise English scene description for AI image generation.
                        Keep visual details only.
                        Do not include any quoted text/dialogue.
                        Do not ask for speech bubbles, signs, labels, letters, or numbers.
                        Return exactly one line.
                        """)
                .userMessage(topicPrompt)
                .build();
        try {
            AiResponse response = aiContentTextGenerator.generate(aiRequest);
            String translated = response.content() == null ? "" : response.content().trim();
            return translated.isBlank() ? topicPrompt : translated;
        } catch (Exception ex) {
            log.warn("[ContentImageV2] Scene prompt normalization failed, using original prompt: {}", ex.getMessage());
            return topicPrompt;
        }
    }

    private String buildSyncedImageScenePrompt(String topicPrompt, String overlayText) {
        String topicScene = normalizeScenePromptForImage(topicPrompt);
        String captionScene = normalizeScenePromptForImage(overlayText);
        if (isBlank(topicScene)) {
            return captionScene;
        }
        if (isBlank(captionScene)) {
            return topicScene;
        }
        return """
                Main scene (highest priority, must follow this first): %s
                Caption intent to align with: %s
                Keep both lines semantically consistent; do not switch to unrelated people, places, or events.
                Prefer a single clear subject connected to the main scene, cinematic social-media composition.
                """.formatted(topicScene, captionScene);
    }

    private String translateToMyanmar(String text, boolean strict) {
        String systemPrompt = strict
                ? """
                You are a professional Burmese copywriter.
                Translate the given headline into simple, clear Burmese (Myanmar Unicode).
                Return exactly one short line suitable for image overlay (max 10 words).
                Make it sound natural for social media users in Myanmar.
                Keep proper nouns unchanged only when needed.
                No English sentence, no markdown, no quotes, no explanation.
                """
                : """
                Translate the given headline into simple, natural Burmese (Myanmar Unicode).
                Return exactly one short line (max 8 words).
                No markdown, no quotes, no extra labels.
                """;

        AiRequest aiRequest = AiRequest.builder()
                .provider(AiProvider.GEMINI)
                .systemPrompt(systemPrompt)
                .userMessage(text)
                .build();
        try {
            AiResponse response = aiContentTextGenerator.generate(aiRequest);
            String translated = response.content() == null ? "" : response.content().trim();
            return translated.isBlank() ? text : translated;
        } catch (Exception ex) {
            log.warn("[ContentImageV2] Myanmar translation failed, using original text: {}", ex.getMessage());
            return text;
        }
    }

    private String sanitizeCaption(String value) {
        String clean = value == null ? "" : value.trim();
        clean = clean.replace("`", "")
                .replace("\"", "")
                .replace("*", "")
                .replace("[TITLE]:", "")
                .replace("[CONTENT]:", "");
        clean = clean.replaceAll("[\\r\\n]+", " ").trim();
        // Remove outer brackets/parentheses often returned by LLM formatting.
        if ((clean.startsWith("(") && clean.endsWith(")"))
                || (clean.startsWith("（") && clean.endsWith("）"))
                || (clean.startsWith("[") && clean.endsWith("]"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        clean = clean.replaceAll("^\\(+|\\)+$", "").trim();
        return clean.isBlank() ? "AI ဖန်တီးမှု" : clean;
    }

    private String normalizeMyanmarCaption(String caption) {
        if (isBlank(caption)) {
            return "AI ဖန်တီးမှု";
        }
        AiRequest aiRequest = AiRequest.builder()
                .provider(AiProvider.GEMINI)
                .systemPrompt("""
                        Rewrite the input as very clear, natural Burmese for an image caption.
                        Keep original meaning.
                        Use one short sentence only (max 8 words).
                        Must be easy to read and make immediate sense.
                        No parentheses, no brackets, no quotes, no emojis.
                        Return Burmese Unicode text only.
                        """)
                .userMessage(caption)
                .build();
        try {
            AiResponse response = aiContentTextGenerator.generate(aiRequest);
            String rewritten = response.content() == null ? "" : response.content().trim();
            return sanitizeCaption(rewritten);
        } catch (Exception ex) {
            log.warn("[ContentImageV2] Myanmar caption normalization failed, using original: {}", ex.getMessage());
            return sanitizeCaption(caption);
        }
    }

    private String toDataUri(String mimeType, byte[] bytes) {
        String mime = isBlank(mimeType) ? "image/png" : mimeType.trim();
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
