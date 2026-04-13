package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.generator.AiVoiceOverGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.prompt.impl.PromptBuilderImpl;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.ai.storage.AudioStorageService;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
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

    private final PromptBuilderImpl promptBuilderImpl;
    private final AiVoiceOverGenerator aiVoiceOverGenerator;
    private final AiContentTextGenerator aiContentTextGenerator;
    private final AudioStorageService audioStorageService;

    private static final String DEFAULT_SOURCE = "English";
    private static final String DEFAULT_TARGET = "Myanmar";
    private static final String DEFAULT_STYLE  = "Formal";
    private static final String DEFAULT_AI_MODEL = "Alex";
    private static final String DEFAULT_TEXT_LENGTH = "Short";

    private static final String TAG_TITLE  = "[TITLE]:";
    private static final String TAG_SCRIPT = "[SCRIPT]:";

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
                .featureType(FeatureType.GENERATE_CONTENT_TEXT)
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

        log.info("[VoiceOver] source={} target={} style={} voice={} textLength={}",
                source, target, style, aiModel, textLength);

        // ── Step 1: Let LLM clean/enhance the script ─────────────────────────
        // aiModel here is the TTS voice (alloy, nova, etc.)
        // We use a separate text-model just for script refinement
        String refinedScript = refineScript(req.text(), source, target, style, textLength , req.provider());

        // ── Step 2: Send refined script to TTS ───────────────────────────────
        double speed = resolveSpeed(textLength);
        byte[] audioBytes = aiVoiceOverGenerator.generateAudio(refinedScript, aiModel, speed);

        // ── Step 3: Save audio and build response ─────────────────────────────
        String audioUrl = audioStorageService.save(audioBytes); // see Note below

        return VoiceOverResponse.builder()
                .audioUrl(audioUrl)
                .sourceLanguage(source)
                .targetLanguage(target)
                .style(style)
                .rawOutput(refinedScript)
                .usedProvider(req.provider())
                .build();
    }

    /**
     * Uses the chat LLM to clean up and naturalise the script before TTS.
     * For very short inputs like "နေကောင်းလား" this may return the text as-is.
     */
    private String refineScript(String text, String source, String target,
                                String style, String textLength, AiProvider provider) {
        AiRequest refineRequest = AiRequest.builder()
                .systemPrompt(promptBuilderImpl.buildVoiceOverPrompt(
                        source, target, style, DEFAULT_AI_MODEL, textLength))
                .userMessage(text)
                .provider(provider) // use default provider
                .build();

        AiResponse refined = aiContentTextGenerator.generate(refineRequest);

        // Extract [SCRIPT]: block; fall back to raw text if parsing fails
        String script = parseTag(refined.content(), TAG_SCRIPT, null);
        return isBlank(script) ? text : script;
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

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolve(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    /**
     * Extracts the content between {@code startTag} and {@code endTag} (or end-of-string).
     * E.g. parseTag(raw, "[TITLE]:", "[SCRIPT]:") → "Myanmar Night Market"
     */
    private String parseTag(String raw, String startTag, String endTag) {
        int start = raw.indexOf(startTag);
        if (start == -1) return "";
        start += startTag.length();

        int end = (endTag != null) ? raw.indexOf(endTag, start) : raw.length();
        if (end == -1) end = raw.length();

        return raw.substring(start, end).strip();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

}
