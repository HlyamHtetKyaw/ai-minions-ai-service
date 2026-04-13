package com.aiminion.aiservice.common.ai.generator;

import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
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
public class AiVoiceOverGenerator {

    private final Map<AiProvider, TtsClient> clients;

    @Value("${ai.tts-default-provider:GEMINI}")
    private AiProvider defaultProvider;

    public AiVoiceOverGenerator(List<TtsClient> ttsClients) {
        this.clients = ttsClients.stream()
                .collect(Collectors.toMap(TtsClient::getProvider, Function.identity()));
        log.info("[AiVoiceOverGenerator] Registered TTS providers: {}", this.clients.keySet());
    }

    public byte[] generateAudio(AiProvider provider, String text, String voice, double speed) {
        AiProvider resolved = provider != null ? provider : defaultProvider;

        TtsClient client = clients.get(resolved);
        if (client == null) {
            throw new IllegalArgumentException(
                    "No TTS client registered for provider: " + resolved);
        }

        log.info("[AiVoiceOverGenerator] Using provider={} voice={} speed={}", resolved, voice, speed);
        return client.synthesize(text, voice, speed);
    }
}