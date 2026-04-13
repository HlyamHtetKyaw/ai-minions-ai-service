package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.enums.AiProvider;

public interface TtsClient {
    AiProvider getProvider();
    byte[] synthesize(String text, String voice, double speed);
}
