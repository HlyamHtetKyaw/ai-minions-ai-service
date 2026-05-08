package com.aiminion.aiservice.common.ai.clientProvider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.stereotype.Component;

import com.aiminion.aiservice.common.ai.context.UserGeminiApiKeyContext;
import com.google.genai.Client;

@Component
public class GeminiRuntimeChatModelFactory {

	public ChatModel resolve(ChatModel defaultModel) {
		String key = UserGeminiApiKeyContext.get();
		if (key == null || key.isBlank()) {
			return defaultModel;
		}
		Client genAiClient = Client.builder()
				.apiKey(key)
				.build();
		return GoogleGenAiChatModel.builder()
				.genAiClient(genAiClient)
				.build();
	}
}
