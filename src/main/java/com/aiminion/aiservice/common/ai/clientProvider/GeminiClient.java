package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient implements AiClient {

    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AiResponse chat(AiRequest request) {
        String url = apiUrl + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", request.systemPrompt()))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", request.userMessage())))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class
            );
            return AiResponse.builder()
                    .content(extractContent(response.getBody()))
                    .usedProvider(AiProvider.GEMINI)
                    .build();
        } catch (Exception ex) {
            log.error("[Gemini] Call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Gemini service unavailable. Please try again later.");
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> body) {
        try {
            List<Map<?, ?>> candidates = (List<Map<?, ?>>) body.get("candidates");
            Map<?, ?> content = (Map<?, ?>) candidates.get(0).get("content");
            List<Map<?, ?>> parts = (List<Map<?, ?>>) content.get("parts");
            return ((String) parts.get(0).get("text")).trim();
        } catch (Exception ex) {
            log.error("[Gemini] Failed to parse response: {}", body);
            throw new RuntimeException("Unexpected response format from Gemini.");
        }
    }
}