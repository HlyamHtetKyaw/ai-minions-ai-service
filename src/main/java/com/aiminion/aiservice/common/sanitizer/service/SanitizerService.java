package com.aiminion.aiservice.common.sanitizer.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.prompt.impl.PromptBuilderImpl;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.exception.ContentViolationException;
import com.aiminion.aiservice.common.sanitizer.dto.SanitizationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SanitizerService {

    private final PromptBuilderImpl      promptBuilderImpl;
    private final AiContentTextGenerator aiContentTextGenerator;
    private final ObjectMapper           objectMapper;

    @Value("${ai.tts-default-provider:GEMINI}")
    private AiProvider defaultProvider;

    private static final int SHORT_TEXT_THRESHOLD = 60;
    private static final List<String> TRIGGER_KEYWORDS = List.of(
            "bomb", "kill", "hack", "exploit", "synthesize", "porn",
            "suicide", "weapon", "ignore previous", "ignore instructions",
            "override", "jailbreak", "disregard", "drug", "murder", "rape"
    );

    public void sanitize(String content, FeatureType featureType, AiProvider provider) {
        if (content == null || content.isBlank()) {
            log.debug("[Sanitizer] Empty content — skipping.");
            return;
        }

        // Fast local pre-check — no AI call needed for obviously safe content
        if (isClearlyHarmless(content)) {
            log.debug("[Sanitizer] Fast-pass — skipped AI check for short/clean content.");
            return;
        }

        provider = (provider == null) ? defaultProvider : provider;
        runAiSanitizationCheck(content, featureType, provider);
    }

    private boolean isClearlyHarmless(String content) {
        String lower = content.toLowerCase();
        boolean hasTrigger = TRIGGER_KEYWORDS.stream().anyMatch(lower::contains);
        if (hasTrigger) return false;
        return content.length() <= SHORT_TEXT_THRESHOLD;
    }

    private void runAiSanitizationCheck(String content, FeatureType featureType, AiProvider provider) {
        try {
            AiRequest aiRequest = AiRequest.builder()
                    .systemPrompt(promptBuilderImpl.buildSanitizationPrompt(content, featureType))
                    .provider(provider)
                    .userMessage("Classify the text provided in the system prompt.")
                    .build();

            AiResponse aiResponse = aiContentTextGenerator.generate(aiRequest);
            String raw = aiResponse.content();

            log.debug("[Sanitizer] Raw AI verdict feature={}: {}", featureType, raw);

            SanitizationResult result = parse(raw);

            if (!result.safe()) {
                log.warn("[Sanitizer] BLOCKED — feature={} category={} reason={}",
                        featureType, result.category(), result.reason());
                throw new ContentViolationException(result.category(), result.reason());
            }

            log.info("[Sanitizer] PASSED — feature={} category={}", featureType, result.category());

        } catch (ContentViolationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[Sanitizer] Unexpected error — feature={} provider={}", featureType, provider, ex);
            throw new IllegalStateException("Unable to validate content at this time. Please try again.");
        }
    }

    private SanitizationResult parse(String raw) {
        try {
            String cleaned = raw.replaceAll("```[a-z]*", "").replace("```", "").trim();
            return objectMapper.readValue(cleaned, SanitizationResult.class);
        } catch (Exception e) {
            log.error("[Sanitizer] Failed to parse AI verdict — failing open. raw={}", raw, e);
            return new SanitizationResult(true, "Parse error — failing open", "NONE");
        }
    }
}