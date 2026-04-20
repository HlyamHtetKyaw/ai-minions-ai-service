package com.aiminion.aiservice.common.ai.clientProvider.impl;
import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class OpenAiTtsClient implements TtsClient {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.tts-api-url:https://api.openai.com/v1/audio/speech}")
    private String ttsApiUrl;

    @Value("${openai.tts-model:gpt-4o-mini-tts}")
    private String ttsModel;

    public OpenAiTtsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public VoiceOverResponse synthesize(String text, String voice, double speed) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model",  ttsModel,
                "input",  text,
                "voice",  voice,
                "format", "mp3",
                "speed",  speed
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    ttsApiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    byte[].class
            );

            log.info("[OpenAiTtsClient] TTS success, bytes={}",
                    response.getBody() != null ? response.getBody().length : 0);

            byte[] audioBytes = response.getBody();

            int tokenIn = estimateTokens(text);
            int tokenOut = 0;

            return VoiceOverResponse.builder()
                    .audioByte(audioBytes)
                    .audioBase64(Base64.getEncoder().encodeToString(audioBytes))
                    .usedProvider(AiProvider.OPENAI)
                    .tokenIn(tokenIn)
                    .tokenOut(tokenOut)
                    .build();

        } catch (Exception ex) {
            log.error("[OpenAiTtsClient] TTS call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("OpenAI voice generation unavailable. Please try again later.");
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }
}