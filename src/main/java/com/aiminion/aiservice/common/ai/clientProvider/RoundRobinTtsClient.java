package com.aiminion.aiservice.common.ai.clientProvider;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceModelDescriptor;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps N TtsClient delegates for the same provider and distributes
 * synthesize() calls across them in a thread-safe round-robin fashion.
 *
 * listVoiceModels() delegates to the first client (all share the same catalog).
 */
@Slf4j
public class RoundRobinTtsClient implements TtsClient {

    private final AiProvider provider;
    private final List<TtsClient> delegates;
    private final AtomicInteger counter = new AtomicInteger(0);

    public RoundRobinTtsClient(AiProvider provider, List<TtsClient> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("RoundRobinTtsClient requires at least one delegate.");
        }
        this.provider  = provider;
        this.delegates = List.copyOf(delegates);  // immutable snapshot
    }

    @Override
    public AiProvider getProvider() {
        return provider;
    }

    @Override
    public VoiceOverResponse synthesize(String text, String voice, double speed) {
        // Mod with size keeps index in range even after Integer overflow
        int idx    = Math.abs(counter.getAndIncrement() % delegates.size());
        TtsClient  chosen = delegates.get(idx);

        log.info("[RoundRobinTtsClient] provider={} slot={}/{} delegate={}",
                provider, idx + 1, delegates.size(), chosen.getClass().getSimpleName());

        return chosen.synthesize(text, voice, speed);
    }

    @Override
    public List<VoiceModelDescriptor> listVoiceModels() {
        // All delegates share the same voice catalog — first one is canonical
        return delegates.get(0).listVoiceModels();
    }
}
