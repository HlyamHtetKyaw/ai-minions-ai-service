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
        String source      = params[0];
        String target      = params[1];
        String contentType = params[2];
        String tone        = params[3];

        String contentTypeInstruction = buildContentTypeInstruction(contentType);

        return String.format("""
    ### Role
    Expert Bilingual Content Strategist and Native %s Linguist.

    ### Task
    Localize and transcreate the input text from %s to %s.
    Generate the content as: **%s**

    ### Content Type Instructions
    %s

    ### Style & Tone
    - Tone: %s
    - Audience: Native speakers of %s.
    - Goal: Avoid "translation-ese." Ensure the text feels like it was originally written in %s.

    ### Technical Requirements
    1. Accuracy: Preserve 100%% of semantic meaning and cultural nuances.
    2. Format: Maintain original line breaks, Markdown formatting, and bullet structures.
    3. Character Encoding: Use standard Unicode (UTF-8) for Burmese script.

    ### Output Format (Strictly Follow)
    Return your response in the following format:

    [TITLE]: (A short, evocative title suitable for an image generation prompt)
    [CONTENT]: (The generated %s content only — no extra commentary)

    Text to process:
    """, target, source, target, contentType, contentTypeInstruction, tone, target, target, contentType);
    }

    /**
     * Returns content-type-specific writing instructions for the AI.
     */
    private String buildContentTypeInstruction(String contentType) {
        return switch (contentType.toUpperCase()) {
            case "HOOK" -> """
                - Write a single punchy opening line that grabs attention immediately.
                - Use curiosity, surprise, or bold statements.
                - Maximum 2 sentences. No hashtags.
                """;
            case "CAPTION" -> """
                - Write an engaging social media caption.
                - Include a call-to-action at the end (e.g. "Share your thoughts below").
                - Keep it concise: 3-5 sentences max.
                """;
            case "HASHTAG" -> """
                - Generate a list of 10-15 relevant hashtags only.
                - Mix popular and niche hashtags.
                - Format: each hashtag on a new line, starting with #.
                - No sentences, no explanation — hashtags only.
                """;
            case "BLOG" -> """
                - Write a full blog-style article with clear paragraphs.
                - Include an introduction, body, and conclusion.
                - Use headers where appropriate.
                """;
            case "SUMMARY" -> """
                - Summarize the content in 2-3 concise sentences.
                - Capture the key message only.
                - No bullet points, no headers.
                """;
            case "THREAD" -> """
                - Write a Twitter/X-style thread.
                - Format as numbered tweets: 1/, 2/, 3/ etc.
                - Each tweet must be under 280 characters.
                - 5-7 tweets total.
                """;
            default -> String.format("""
                - Write the content as a "%s".
                - Keep it relevant, concise, and engaging.
                """, contentType);
        };
    }
}