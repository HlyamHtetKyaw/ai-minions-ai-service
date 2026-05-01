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
        String styleInstruction = buildTranslateStyleInstruction(style, target);

        return String.format("""
                You are a professional translator with deep expertise in %s and %s linguistics.

                Your task:
                - Translate the user's text from %s → %s.
                - Apply a "%s" tone/style consistently throughout.
                %s
                - Preserve the original meaning, nuance, and formatting (line breaks, punctuation).
                - For Myanmar/Burmese output, use Unicode (UTF-8) script.
                - Return ONLY the translated text — no explanations, no labels, no extra content.
                """, source, target, source, target, style, styleInstruction);
    }

    private String buildTranslateStyleInstruction(String style, String target) {
        String normalizedStyle = style == null ? "" : style.trim().toLowerCase();
        String normalizedTarget = target == null ? "" : target.trim().toLowerCase();
        boolean targetIsBurmese = normalizedTarget.contains("burmese") || normalizedTarget.contains("myanmar");

        if (!targetIsBurmese) {
            return "- Match the requested style while keeping grammar and word choice native to the target language.";
        }

        return switch (normalizedStyle) {
            case "casual_social_media" -> """
                - Burmese diglossia rule (MANDATORY): use spoken Burmese (စကားပြောဟန် / Zaga Pyaw), not literary Burmese.
                - Keep the output natural and conversational for social media; use spoken particles (e.g. တယ်, ရဲ့, ပြီး) and avoid literary particles (e.g. သည်, ၏, ၍).
                - The voice should sound like a creator speaking naturally to friends or followers.
                """;
            case "polite_educational" -> """
                - Burmese diglossia rule (MANDATORY): use spoken Burmese (စကားပြောဟန် / Zaga Pyaw), not literary Burmese.
                - Keep a polite, respectful educational tone for audience-facing content; frequently use polite particles such as "ပါ" where natural.
                - Do not drift into stiff literary grammar.
                """;
            case "formal_corporate" -> """
                - Burmese diglossia rule (MANDATORY): use literary Burmese (စာပေဟန် / Sape) consistently.
                - Use formal, professional wording and literary particles (e.g. သည်, ၏, ၍) suitable for official business communication.
                - Do not mix spoken colloquial grammar into this mode.
                """;
            case "youthful_trendy" -> """
                - Burmese diglossia rule (MANDATORY): use spoken Burmese (စကားပြောဟန် / Zaga Pyaw), not literary Burmese.
                - Keep the style punchy, youthful, and trend-aware; modern slang or selective transliterated English terms are allowed when natural.
                - Maintain readability and avoid mixing incompatible spoken/literary grammar in the same sentence.
                """;
            default -> """
                - Burmese diglossia rule (MANDATORY): choose one grammar register and stay consistent.
                - Prefer spoken Burmese (စကားပြောဟန် / Zaga Pyaw) unless the user explicitly asks for literary formal output.
                - Do not mix spoken and literary particles in a single sentence.
                """;
        };
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
                LONG-FORM HOOK ("story builder") — one cohesive deliverable only (LinkedIn, blogs, newsletters, long YouTube).
                - Produce **a single** opening: either one short paragraph or a few tight spaced lines that pull the reader into a longer piece (conversational, authoritative; build context — not a gimmick alone).
                - Do **not** offer multiple alternative hooks. Do **not** label "Option 1", "Option 2", or A/B variants.
                - Use storytelling patterns where natural: problem → tension → hint of resolution; relatable scenario; credible statistic; or brief framing (no fake biographies).
                - Then, still as the same unified output, add:
                  **Suggested outline:** (3-6 bullet points for how the rest of the piece could unfold)
                  **Transition line:** (one sentence bridging the hook into the first body section)
                - No hashtags in the hook itself. Keep length appropriate for an opening — not a full article.
                """ : """
                SHORT-FORM HOOK ("scroll stopper") — one cohesive deliverable only (TikTok, Reels, Shorts, X).
                - Produce **exactly one** hook: **1–2 sentences** total, maximum impact. No alternatives, no numbered variants, no "Option" labels.
                - Aim for immediate pattern interruption: curiosity gap, bold claim, direct question, or mid-action opener — pick the single strongest angle for the topic.
                - No fluff, no preamble, no hashtags. Ultra-fast pacing; every word earns attention in the first few seconds on-screen or read aloud.
                """;
            case "CAPTION" -> longForm ? """
                LONG CAPTION ("micro-blog") — one cohesive caption only (Instagram, LinkedIn, Facebook, educational carousels, talking-head clips).
                - The copy must **add context, story or lesson, and authority**; you may use **natural, searchable phrasing** in the prose — but **do not** include hashtags: no `#` tokens, no trailing keyword stacks, no "hashtag blocks."
                - Do **not** output multiple alternative captions. Do **not** label "Caption A/B" or "Option" variants.
                - **Read-more hook:** The very first line (before the first intentional line break) must work as a standalone hook and stay roughly **under 125 characters** so it survives "Read more" truncation on typical feeds.
                - **Formatting (mandatory):** Use deliberate line breaks between beats; short paragraphs; optional bullet lines (e.g. 👉) for steps or lists. **Never** output one giant unbroken wall of text.
                - Single flow: opening hook line → body (story, lesson, or value) → clear **CTA** (comment, save, share, link in bio, etc.). End on the CTA or a closing line — **no hashtags**.
                - Tone: educational, personal, or authoritative as fits the topic; emojis allowed where they aid scanability (not spammy).
                """ : """
                SHORT CAPTION ("quick context") — **one** cohesive caption only; the image or video does most of the work (TikTok, aesthetic Instagram, Pinterest).
                - Produce **exactly one** caption: **1–2 punchy lines** plus **emojis** where they set tone. Include a **simple CTA** (e.g. link in bio, save for later, tag someone). **Do not** include hashtags (`#`) or hashtag-style keyword lists.
                - Do **not** offer multiple variations. Do **not** label "Option 1", "Option 2", or numbered alternatives.
                - Highly scannable; no long paragraphs; no preamble explaining what you are doing.
                """;
            case "SCRIPT" -> longForm ? """
                LONG SCRIPT — YouTube long-form, podcasts, video essays, deep educational pieces (goal: watch-time and clear structure).
                - **Length:** Target roughly **1,000–1,800 words** of spoken content (about **8–15+ minutes** at a natural talking pace; adjust slightly if the topic demands, but do not collapse into a short reel).
                - **Tone:** Conversational, authoritative where appropriate; strategic pauses implied through short sentences and new paragraphs; **no wall of text** — the creator must find their place while filming or recording.
                - **Structure (mandatory):** Use a **production-style layout** with clear acts/chapters, for example:
                  - First line inside [CONTENT]: `Title Idea:` followed by a compelling working title for the episode.
                  - Then sections such as `--- ACT 1: The Hook (0:00 - 1:00) ---`, `--- ACT 2: ... ---`, etc. Include **Intro / branding beat**, **2+ core chapters** for the main argument or story, and a **Conclusion / outro** with CTA.
                - **Visual / B-roll cues:** Before blocks of dialogue, use bracketed lines such as `[B-Roll: ...]`, `[Visual: ...]`, or `[On-screen: ...]` so the speaker knows what appears while they talk.
                - **Spoken lines:** Prefix dialogue with `Spoken:` (or `Audio:`) on its own line, then the line(s) to say aloud. Keep vocabulary natural for speech.
                - **Content:** Story arc (hook/teaser → setup → development → payoff → outro). Weave the user's topic from "Text to process" throughout; expand with examples, transitions, and signposting between acts.
                - Avoid hashtag-only lines; hashtags are optional only if natural in spoken CTA.
                """ : """
                SHORT SCRIPT — TikTok, YouTube Shorts, Instagram Reels (goal: **high retention**, **fast pace**, **30–60 seconds** spoken — roughly **100–150 words** total).
                - **Pacing:** **Extremely strong hook in the first ~3 seconds** of spoken time; **zero fluff**; **rapid cuts** implied between beats; end with a **quick CTA** (comment, follow, link, save).
                - **Vocabulary:** Short, simple words; **avoid** tongue-twisters and dense jargon the speaker must rush through.
                - **Format (teleprompter-ready):** Alternate **one line of bracketed visual** then **one `Audio:` line** with the spoken words in quotes. Example shape (adapt to topic):
                  `[Visual: ...]`
                  `Audio: "..."`
                  Use **clear [Visual: ...] cues** (camera, expression, prop, quick cut, screen recording) so the speaker reads dialogue cleanly without tripping over notes.
                - **Structure:** Hook → 2–4 tight beats (each beat = visual + audio) → **final beat = CTA**. No long paragraphs of narration without a visual line between them.
                - Ground every line in the user's topic from "Text to process"; do not pad with generic filler.
                - Avoid hashtag-only lines.
                """;
            case "HASHTAG", "HASHTAGS" -> longForm ? """
                HASHTAGS — **LONG list** mode. Output **hashtags only** (each line one tag starting with `#`). No prose, no numbering explanations, no markdown fences.
                **Step 1 — Classify the user's topic** (the text under "Text to process") as **SHORT INPUT** (~one short phrase, few words, little context) vs **LONG INPUT** (full sentence, multiple sentences, bullet list, or paragraph with detail).
                **SHORT INPUT (extrapolate):** The seed is thin; do **not** only repeat the literal words. Brainstorm a **semantic web**: related industries, audiences, platforms, synonyms, adjacent niches, and long-tail angles (e.g. from "new app launch" branch toward communities and categories like #IndieDev, #SaaS, #ProductHunt, #BuildInPublic where relevant to the topic). Output **at least 25** and **at most 40** hashtags. Mix broader discovery tags with specific long-tail tags.
                **LONG INPUT (extract & organize):** First **distill** the core subject, audience, and primary outcomes; **ignore** filler, small talk, and off-topic tangents (e.g. do not tag a casual "walk" if the piece is really about shipping code). Pull **named tools, frameworks, products, locations, roles**, and distinctive phrases when they matter. Output **22–35** hashtags. Group related tags with **blank lines only** between groups (no section titles or sentences).
                """ : """
                HASHTAGS — **SHORT list** mode. Output **hashtags only** (each line one tag starting with `#`). No prose, no markdown fences.
                **Step 1 — Classify the user's topic** as **SHORT INPUT** (brief seed, little context) vs **LONG INPUT** (detailed text with multiple ideas).
                **SHORT INPUT (extrapolate, but keep the list compact):** Output **three groups** of hashtags separated by a **single blank line** between groups (hashtag lines only—no words or labels). Each group contains **exactly 3** hashtags (one `#` tag per line), **9 hashtags total**. Each group must reflect a **different inferred angle** (e.g. industry vs audience vs platform vs outcome) so the clusters feel distinct; extrapolate beyond the literal seed words.
                **LONG INPUT (distill):** Extract only the **3–5** most critical themes (core subject, audience, outcome). **Ignore** conversational filler. Output **10–15** hashtags total, one per line — each tag must map to those distilled themes; no irrelevant tags from digressions.
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