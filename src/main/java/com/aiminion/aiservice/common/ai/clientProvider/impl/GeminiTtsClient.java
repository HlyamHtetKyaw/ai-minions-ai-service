package com.aiminion.aiservice.common.ai.clientProvider.impl;

import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiTtsClient implements TtsClient {

    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.tts-model:gemini-2.5-flash-preview-tts}")
    private String ttsModel;

    private static final String GEMINI_TTS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public GeminiTtsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    /**
     * @param voice  Gemini prebuilt voice: Aoede | Charon | Fenrir | Kore | Puck | Zephyr …
     * @param speed  reserved for future use (Gemini TTS doesn't expose speed yet)
     */
    @Override
    public byte[] synthesize(String text, String voice, double speed) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", text)))
                ),
                "generationConfig", Map.of(
                        "responseModalities", List.of("AUDIO"),
                        "speechConfig", Map.of(
                                "voiceConfig", Map.of(
                                        "prebuiltVoiceConfig", Map.of(
                                                "voiceName", voice
                                        )
                                )
                        )
                )
        );

        String url = String.format(GEMINI_TTS_URL, ttsModel, apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            String audioBase64 = extractAudioData(response.getBody());
            log.info("[GeminiTtsClient] TTS success, voice={}", voice);
            return Base64.getDecoder().decode(audioBase64);

        } catch (Exception ex) {
            log.error("[GeminiTtsClient] TTS call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Gemini voice generation unavailable. Please try again later.");
        }
    }

    /**
     * Response path:
     * candidates[0] → content → parts[0] → inlineData → data (base64 audio)
     */
    @SuppressWarnings("unchecked")
    private String extractAudioData(Map<?, ?> body) {
        try {
            List<Map<?, ?>> candidates = (List<Map<?, ?>>) body.get("candidates");
            Map<?, ?> content         = (Map<?, ?>) candidates.get(0).get("content");
            List<Map<?, ?>> parts     = (List<Map<?, ?>>) content.get("parts");
            Map<?, ?> inlineData      = (Map<?, ?>) parts.get(0).get("inlineData");
            return (String) inlineData.get("data");
        } catch (Exception ex) {
            log.error("[GeminiTtsClient] Failed to parse audio response: {}", body);
            throw new RuntimeException("Unexpected response format from Gemini TTS.");
        }
    }
}