package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceModelDescriptor;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;

import java.util.List;

public interface TtsClient {
    AiProvider getProvider();
    VoiceOverResponse synthesize(String text, String voice, double speed);

    List<VoiceModelDescriptor> listVoiceModels();
}
