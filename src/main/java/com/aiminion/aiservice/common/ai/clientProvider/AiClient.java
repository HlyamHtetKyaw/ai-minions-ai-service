package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;

public interface AiClient {

    /**
     * Sends a prompt to the AI provider and returns the text response.
     *
     * @param systemPrompt  Instructions/context for the AI
     * @param userMessage   The actual user content to process
     * @return              Raw text response from the AI
     */
    AiResponse chat(AiRequest request);

    /**
     * Identifies which provider this client handles.
     */
    AiProvider getProvider();
}