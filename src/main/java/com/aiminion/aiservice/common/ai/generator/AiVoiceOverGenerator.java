package com.aiminion.aiservice.common.ai.generator;

import com.aiminion.aiservice.common.ai.clientProvider.AiClient;
import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiVoiceOverGenerator {

    private final Map<AiProvider, AiClient> aiClients;
    private final Map<AiProvider, TtsClient> ttsClients;

    @Value("${ai.tts-default-provider:GEMINI}")
    private AiProvider defaultProvider;

    public AiVoiceOverGenerator(List<AiClient> aiClients, List<TtsClient> ttsClients) {
        this.aiClients = aiClients.stream()
                .collect(Collectors.toMap(AiClient::getProvider, Function.identity()));
        this.ttsClients = ttsClients.stream()
                .collect(Collectors.toMap(TtsClient::getProvider, Function.identity()));
        log.info("[AiVoiceOverGenerator] Registered AI  providers: {}", this.aiClients.keySet());
        log.info("[AiVoiceOverGenerator] Registered TTS providers: {}", this.ttsClients.keySet());
    }

    public VoiceOverResponse generateAudio(AiProvider provider, String text, String voice, double speed) {
        AiProvider resolved = provider != null ? provider : defaultProvider;

        TtsClient client = ttsClients.get(resolved);
        if (client == null) {
            throw new IllegalArgumentException(
                    "No TTS client registered for provider: " + resolved);
        }

        log.info("[AiVoiceOverGenerator] Using provider={} voice={} speed={}", resolved, voice, speed);
        return client.synthesize(text, voice, speed);
    }

    /**
     * Directly calls the raw AiClient (e.g. GeminiClient) with a structured
     * system prompt. Bypasses AiContentTextGenerator which overwrites userMessage.
     */
    public List<String> generate(AiRequest aiRequest) {
        AiProvider resolved = aiRequest.provider() != null
                ? aiRequest.provider()
                : defaultProvider;

        AiClient client = aiClients.get(resolved);
        if (client == null) {
            log.warn("[AiVoiceOverGenerator] No AI client for provider={}, using fallback list", resolved);
            return fallbackVoiceModels();
        }

        log.info("[AiVoiceOverGenerator] Fetching latest voice models via provider={}", resolved);

        // Direct chat call — systemPrompt + userMessage both guaranteed non-blank
        AiResponse aiResponse = client.chat(aiRequest);
        String rawJson = aiResponse.content().strip();

        log.debug("[AiVoiceOverGenerator] Raw model list response: {}", rawJson);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("[AiVoiceOverGenerator] Failed to parse voice model list: {}", rawJson, e);
            return fallbackVoiceModels();
        }
    }

    private List<String> fallbackVoiceModels() {
        return List.of("Zephyr", "Puck", "Charon", "Kore",
                "Fenrir", "Aoede", "Leda", "Orus", "Schedar");
    }
}