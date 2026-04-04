package com.aiminion.aiservice.common.ai.prompt.impl;

import com.aiminion.aiservice.common.ai.prompt.PromptBuilder;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilderImpl implements PromptBuilder {
    /**
     * Builds the translation system prompt.
     *
     * @param params [0] source language
     *               [1] target language
     *               [2] style (Formal, Casual, etc.)
     */
    @Override
    public String buildTranslatePrompt(String... params) {
        String source = params[0];
        String target = params[1];
        String style  = params[2];

        return String.format("""
                You are a professional translator with deep expertise in %s and %s linguistics.

                Your task:
                - Translate the user's text from %s → %s.
                - Apply a "%s" tone/style consistently throughout.
                - Preserve the original meaning, nuance, and formatting (line breaks, punctuation).
                - For Myanmar/Burmese output, use Unicode (UTF-8) script.
                - Return ONLY the translated text — no explanations, no labels, no extra content.
                """, source, target, source, target, style);
    }

    public String buildContentTextPrompt(String... params) {
        String source = params[0];
        String target = params[1];
        String tone = params[2];

        return String.format("""
            Role: Expert Bilingual Content Strategist & Translator (%s and %s).
            
            Task:  
            Localize and rewrite the provided text from %s into %s while maintaining a "%s" tone.
            
            Requirements:
            1. Accuracy: Maintain the exact semantic meaning and cultural nuance.
            2. Language: Use standard Unicode (UTF-8) for Burmese script. Ensure natural flow—avoid "translation-ese."
            3. Formatting: Preserve all original line breaks, bullet points, and special characters.
            4. Constraints: Output ONLY the resulting text. Do not provide greetings, "Here is the translation," or meta-commentary.
            
            Text to process:
            """, source, target, source, target, tone);
    }
}