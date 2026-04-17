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
public class OpenAiClient implements AiClient {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AiResponse chat(AiRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user",   "content", request.userMessage())
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(body, headers), Map.class
            );
            Integer tokenIn = null;
            Integer tokenOut = null;
            try {
                Map<?, ?> usage = (Map<?, ?>) response.getBody().get("usage");
                if (usage != null) {
                    Object pt = usage.get("prompt_tokens");
                    Object ct = usage.get("completion_tokens");
                    if (pt instanceof Number n) tokenIn = n.intValue();
                    if (ct instanceof Number n) tokenOut = n.intValue();
                }
            } catch (Exception ignored) {
            }
            return AiResponse.builder()
                    .content(extractContent(response.getBody()))
                    .usedProvider(AiProvider.OPENAI)
                    .tokenIn(tokenIn)
                    .tokenOut(tokenOut)
                    .build();
        } catch (Exception ex) {
            log.error("[OpenAI] Call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("OpenAI service unavailable. Please try again later.");
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> body) {
        try {
            List<Map<?, ?>> choices = (List<Map<?, ?>>) body.get("choices");
            Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
            return ((String) message.get("content")).trim();
        } catch (Exception ex) {
            log.error("[OpenAI] Failed to parse response: {}", body);
            throw new RuntimeException("Unexpected response format from OpenAI.");
        }
    }
}