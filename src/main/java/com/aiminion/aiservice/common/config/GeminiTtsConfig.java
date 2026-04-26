package com.aiminion.aiservice.common.config;
import com.aiminion.aiservice.common.ai.clientProvider.RoundRobinTtsClient;
import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.ai.clientProvider.impl.GeminiTtsClient;
import com.aiminion.aiservice.common.enums.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Registers a single {@link RoundRobinTtsClient} bean for AiProvider.GEMINI
 * that distributes load across multiple Gemini TTS models.
 *
 * To add or remove a model slot: edit the list below — nothing else changes.
 */
@Configuration
public class GeminiTtsConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.tts-sample-rate-hz:24000}")
    private int pcmSampleRateHz;

    // ── Model identifiers ─────────────────────────────────────────────────────
    @Value("${gemini.tts-model-1:gemini-2.5-flash-preview-tts}")
    private String model1;

    @Value("${gemini.tts-model-2:gemini-2.5-pro-preview-tts}")
    private String model2;

    @Value("${gemini.tts-model-3:gemini-2.0-flash-live-001}")
    private String model3;

    @Bean
    public TtsClient geminiTtsClient(RestTemplate restTemplate) {
        List<TtsClient> delegates = List.of(
                new GeminiTtsClient(restTemplate, apiKey, model1, pcmSampleRateHz),
                new GeminiTtsClient(restTemplate, apiKey, model2, pcmSampleRateHz),
                new GeminiTtsClient(restTemplate, apiKey, model3, pcmSampleRateHz)
        );

        return new RoundRobinTtsClient(AiProvider.GEMINI, delegates);
    }
}