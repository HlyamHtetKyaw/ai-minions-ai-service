package com.aiminion.aiservice.common.ai.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class AiVoiceOverGenerator {

    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${google.tts-api-url:https://texttospeech.googleapis.com/v1/text:synthesize}")
    private String ttsApiUrl;

    @Value("${gemini.tts-model:gemini-1.5-flash}")
    private String ttsModel;

    public AiVoiceOverGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calls OpenAI TTS and returns raw MP3 bytes.
     *
     * @param text       The script to synthesize (Burmese or any language)
     * @param voice      OpenAI voice name: alloy | echo | fable | onyx | nova | shimmer
     * @param speed      Playback speed 0.25 – 4.0  (1.0 = normal)
     */
    public byte[] generateAudio(String text, String voice, double speed) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model",           ttsModel,
                "input",           text,
                "voice",           voice,
                "response_format", "mp3",
                "speed",           speed
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    ttsApiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    byte[].class
            );

            log.info("[AiVoiceOverGenerator] TTS success, bytes={}",
                    response.getBody() != null ? response.getBody().length : 0);

            return response.getBody();

        } catch (Exception ex) {
            log.error("[AiVoiceOverGenerator] TTS call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Voice generation service unavailable. Please try again later.");
        }
    }
}