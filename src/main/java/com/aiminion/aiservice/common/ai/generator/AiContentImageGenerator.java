package com.aiminion.aiservice.common.ai.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiContentImageGenerator {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.image-api-url}")
    private String imageApiUrl;

    @Value("${openai.image-model}")
    private String imageModel;

    public AiContentImageGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateImage(String prompt, String size, String quality) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model",   imageModel,
                "prompt",  prompt,
                "n",       1,
                "size",    size,
                "quality", quality
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    imageApiUrl, new HttpEntity<>(body, headers), Map.class
            );
            return extractImageUrl(response.getBody());
        } catch (Exception ex) {
            log.error("[ImageGenerator] Call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Image generation service unavailable. Please try again later.");
        }
    }

    public String generateImageName() {
        return "img_" + Instant.now().getEpochSecond() + ".png";
    }

    @SuppressWarnings("unchecked")
    private String extractImageUrl(Map<?, ?> body) {
        try {
            List<Map<?, ?>> data = (List<Map<?, ?>>) body.get("data");
            return (String) data.get(0).get("url");
        } catch (Exception ex) {
            log.error("[ImageGenerator] Failed to parse response: {}", body);
            throw new RuntimeException("Unexpected response format from image generation service.");
        }
    }
}
