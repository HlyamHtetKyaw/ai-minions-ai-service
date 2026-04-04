package com.aiminion.aiservice.common.ai.generator;

import com.aiminion.aiservice.common.ai.clientProvider.AiClient;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core AI text generator.
 * All feature services delegate actual AI calls through here.
 */
@Slf4j
@Component
public class AiContentTextGenerator {

    private final Map<AiProvider, AiClient> clients;

    @Value("${ai.default-provider}")
    private AiProvider defaultProvider;

    public AiContentTextGenerator(List<AiClient> aiClients) {
        this.clients = aiClients.stream()
                .collect(Collectors.toMap(AiClient::getProvider, Function.identity()));
        log.info("AiContentTextGenerator registered providers: {}", this.clients.keySet());
    }

    public AiResponse generate(AiRequest request) {
        AiProvider provider = request.provider() != null
                ? request.provider()
                : defaultProvider;

        AiClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("No AI client registered for provider: " + provider);
        }

        log.info("[AiContentTextGenerator] Using provider={}", provider);
        return client.chat(request);
    }
}