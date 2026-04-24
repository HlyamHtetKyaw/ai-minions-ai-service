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
        String textLength  = params.length > 4 && params[4] != null && !params[4].isBlank()
                ? params[4].trim()
                : "SHORT";
        boolean longForm = "LONG".equalsIgnoreCase(textLength);

        String contentTypeInstruction = buildContentTypeInstruction(contentType, longForm);

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
     * Returns content-type-specific writing instructions. {@code longForm} deepens output while
     * keeping the same kind (hook stays hooks, not a blog article).
     */
    private String buildContentTypeInstruction(String contentType, boolean longForm) {
        String key = contentType.toUpperCase();
        // UI sends hook | caption | script | hashtags; API may also use HASHTAG (singular).
        return switch (key) {
            case "HOOK" -> longForm ? """
                - Provide 4-6 distinct hook options the creator can A/B test (label Option 1, Option 2, …).
                - Each option: 1-2 punchy sentences that grab attention (curiosity, contrast, bold claim).
                - No hashtags. Do not write a blog post or essay — only hook lines.
                """ : """
                - Write a single punchy opening line that grabs attention immediately.
                - Use curiosity, surprise, or bold statements.
                - Maximum 2 sentences. No hashtags.
                """;
            case "CAPTION" -> longForm ? """
                - Write a richer social caption: allow up to two short paragraphs if it helps flow.
                - Include a clear call-to-action at the end (e.g. comment, save, share).
                - Aim for roughly 120-220 words unless the topic clearly needs a bit more.
                """ : """
                - Write an engaging social media caption.
                - Include a call-to-action at the end (e.g. "Share your thoughts below").
                - Keep it concise: 3-5 sentences max.
                """;
            case "SCRIPT" -> longForm ? """
                - Write an expanded video script: multiple beats (intro, body sections, outro).
                - Use numbered lines or short scene headings if helpful; keep it readable aloud.
                - Target roughly 2-5 minutes when read aloud (flexible by topic).
                - Avoid hashtag-only lines.
                """ : """
                - Write a short video script or spoken narrative (voiceover / on-camera).
                - Use short paragraphs or numbered lines for beats if it helps clarity.
                - Keep it readable aloud; avoid hashtag-only lines.
                """;
            case "HASHTAG", "HASHTAGS" -> longForm ? """
                - Generate 22-35 relevant hashtags only.
                - Mix popular and niche hashtags.
                - Format: each hashtag on a new line, starting with #.
                - No sentences, no explanation — hashtags only.
                """ : """
                - Generate a list of 10-15 relevant hashtags only.
                - Mix popular and niche hashtags.
                - Format: each hashtag on a new line, starting with #.
                - No sentences, no explanation — hashtags only.
                """;
            case "BLOG" -> longForm ? """
                - Write a full blog-style article with clear paragraphs.
                - Include an introduction, body, and conclusion.
                - Use headers where appropriate.
                """ : """
                - Write a compact article: one clear intro, 2-3 short sections, brief conclusion.
                - Aim for roughly 250-450 words unless the topic needs a little more.
                - Use minimal headings only if they help readability.
                """;
            case "SUMMARY" -> longForm ? """
                - Summarize in 5-7 sentences with a little more nuance than a blurb.
                - Capture key message and one layer of implication; no bullet points unless the topic is list-like.
                """ : """
                - Summarize the content in 2-3 concise sentences.
                - Capture the key message only.
                - No bullet points, no headers.
                """;
            case "THREAD" -> longForm ? """
                - Write a longer Twitter/X-style thread.
                - Format as numbered tweets: 1/, 2/, 3/ etc.
                - Each tweet must be under 280 characters.
                - 8-12 tweets total.
                """ : """
                - Write a Twitter/X-style thread.
                - Format as numbered tweets: 1/, 2/, 3/ etc.
                - Each tweet must be under 280 characters.
                - 5-7 tweets total.
                """;
            default -> longForm ? String.format("""
                - Write the content as a "%s" with more depth and structure than a single paragraph.
                - Stay on-genre for "%s" — do not switch to an unrelated format (e.g. blog) unless that is the type.
                """, contentType, contentType) : String.format("""
                - Write the content as a "%s".
                - Keep it relevant, concise, and engaging.
                """, contentType);
        };
    }

    /**
     * Builds the voiceover system prompt.
     *
     * @param source    source language  (e.g. "Myanmar")
     * @param target    target language  (e.g. "Myanmar")
     * @param style     tone/style       (e.g. "Formal", "Casual", "Dramatic")
     * @param aiModel   voice persona    (e.g. "Alex", "Zara")
     * @param textLength output length   (SHORT | MEDIUM | LONG)
     */
    public String buildVoiceOverPrompt(String source, String target,
                                       String style, String aiModel,
                                       String textLength) {
        String lengthInstruction = switch (textLength.toUpperCase()) {
            case "MEDIUM" -> """
                - Target length: 60–120 seconds when read aloud at a natural pace.
                - Use 2–3 clear sections (intro beat, core message, closing line).
                """;
            case "LONG"   -> """
                - Target length: 2–4 minutes when read aloud at a natural pace.
                - Structure with a clear intro, developed body beats, and a strong outro.
                - You may use short scene cues in brackets (e.g. [pause], [emphasis]) where they help delivery.
                """;
            default       -> // SHORT
                    """
                    - Target length: 15–45 seconds when read aloud at a natural pace.
                    - One tight arc: hook → key message → close. No padding.
                    """;
        };

        return String.format("""
            ### Role
            You are **%s**, a professional voiceover artist and native %s scriptwriter.

            ### Task
            Adapt and rewrite the input text from %s into a polished, broadcast-ready **%s voiceover script**.

            ### Voice & Tone
            - Style  : %s
            - Persona: %s — write in a voice that matches this character's warmth, pace, and cadence.
            - Audience: Native %s speakers; the script must sound completely natural when spoken aloud.

            ### Length & Structure
            %s

            ### Technical Requirements
            1. Spoken-word first: Avoid complex punctuation that reads awkwardly aloud. Prefer em-dashes (—) and ellipses (…) over semicolons/colons.
            2. Burmese script: Use standard Unicode (UTF-8). Do NOT romanise or transliterate.
            3. Preserve meaning: Keep 100%% of the original semantic intent; restructure only for natural delivery.
            4. No stage directions unless LONG mode: Keep [cues] to a minimum.

            ### Output Format (Strictly Follow)
            Return your response in the following format:

            [TITLE]: (A concise title — 3 to 7 words — that captures the essence of the script)
            [SCRIPT]: (The voiceover script only — no extra commentary, no labels inside the script body)
            """,
                aiModel, target,
                source, target,
                style, aiModel, target,
                lengthInstruction);
    }

    public String buildVoiceOverDataPrompt() {
        return """
            ### Role
            You are a knowledgeable AI assistant with up-to-date expertise on Google Gemini's
            text-to-speech (TTS) API offerings.

            ### Task
            Return the latest available Gemini TTS voice model names that can be used
            for voiceover / speech synthesis via the Gemini API.

            ### Output Format (Strictly Follow)
            - Return ONLY a raw JSON array of strings — each string is a voice model name.
            - No markdown, no code fences, no explanations, no preamble.
            - Example of the exact format expected:
              ["Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Aoede"]

            Return the JSON array now:
            """;
    }

    public String buildTranslateStylePrompt() {
        return """
            ### Role
            You are a knowledgeable Translator with up-to-date expertise on Google Gemini's
            Translate API Offerings.

            ### Task
            Return the latest available Gemini Translate model names that can be used
            for Translate synthesis via the Gemini API.

            ### Output Format (Strictly Follow)
            - Return ONLY a raw JSON array of strings — each string is a voice model name.
            - No markdown, no code fences, no explanations, no preamble.
            - Example of the exact format expected:
              ["Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Aoede"]

            Return the JSON array now:
            """;
    }
}