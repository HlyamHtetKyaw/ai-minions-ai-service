package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiClientFactory {

    private final Map<AiProvider, AiClient> clients;

    @Value("${ai.default-provider}")
    private AiProvider defaultProvider;

    // Spring auto-injects ALL AiClient implementations
    public AiClientFactory(List<AiClient> aiClients) {
        this.clients = aiClients.stream()
                .collect(Collectors.toMap(AiClient::getProvider, Function.identity()));
        log.info("Registered AI clients: {}", this.clients.keySet());
    }

    /**
     * Returns the default provider configured in application.properties.
     */
    public AiClient getDefault() {
        return getClient(defaultProvider);
    }

    /**
     * Returns a specific provider — useful when caller explicitly picks one.
     */
    public AiClient getClient(AiProvider provider) {
        AiClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("No AI client registered for provider: " + provider);
        }
        log.debug("Using AI provider: {}", provider);
        return client;
    }
}
