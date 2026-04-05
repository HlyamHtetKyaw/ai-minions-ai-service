package com.aiminion.aiservice.common.ai.prompt;

/**
 * Common contract for all AI prompt builders.
 * Every AI feature must implement this to define its own system prompt.
 */
public interface PromptBuilder {

    /**
     * Builds the system prompt (instructions/context for the AI).
     * @param params  Feature-specific parameters passed as varargs
     * @return        The fully constructed system prompt string
     */
    String buildTranslatePrompt(String... params);
}