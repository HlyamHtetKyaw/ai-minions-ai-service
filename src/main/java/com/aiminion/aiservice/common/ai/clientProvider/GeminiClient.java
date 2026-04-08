package com.aiminion.aiservice.common.ai.clientProvider;

import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GeminiClient implements AiClient {

    private static final int MAX_RETRIES = 3;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 8192;

    private final ChatModel chatModel;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}")
    private String chatModelName;

    @Value("${spring.ai.google.genai.chat.options.temperature:0.2}")
    private double chatTemperature;

    public GeminiClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AiResponse chat(AiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String system = request.systemPrompt() != null ? request.systemPrompt().trim() : "";
        String user = request.userMessage() != null ? request.userMessage().trim() : "";
        if (user.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        if (system.isBlank()) {
            system = "You are a helpful assistant.";
        }

        List<Message> messages = List.of(new SystemMessage(system), new UserMessage(user));

        String model = chatModelName != null && !chatModelName.isBlank()
                ? chatModelName.trim()
                : "gemini-2.0-flash";

        ChatResponse response = null;
        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.info("Gemini text attempt {}/{} model='{}'", attempt + 1, MAX_RETRIES, model);
                var options = GoogleGenAiChatOptions.builder()
                        .model(model)
                        .maxOutputTokens(DEFAULT_MAX_OUTPUT_TOKENS)
                        .temperature(chatTemperature)
                        .build();
                response = chatModel.call(new Prompt(messages, options));
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini text attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted during Gemini retry", ie);
                    }
                }
            }
        }

        if (response == null) {
            if (lastException instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    lastException != null ? lastException.getMessage() : "Unknown Gemini error",
                    lastException);
        }

        String text = response.getResult().getOutput().getText();
        return AiResponse.builder()
                .content(text != null ? text.trim() : "")
                .usedProvider(AiProvider.GEMINI)
                .build();
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }
}