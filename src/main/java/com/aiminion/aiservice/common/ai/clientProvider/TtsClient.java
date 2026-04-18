package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;

public interface TtsClient {
    AiProvider getProvider();
    VoiceOverResponse synthesize(String text, String voice, double speed);
}
