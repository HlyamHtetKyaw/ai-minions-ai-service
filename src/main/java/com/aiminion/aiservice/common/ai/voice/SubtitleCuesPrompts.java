package com.aiminion.aiservice.common.ai.voice;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class SubtitleCuesPrompts {

	public static String userPromptSubtitleCues(
			long chunkDurationMs,
			long chunkOffsetMs,
			int chunkIndex,
			String targetLanguage,
			String styleProfile
	) {
		String lang = targetLanguage == null || targetLanguage.isBlank() ? "my" : targetLanguage.trim();
		String style = styleProfile == null || styleProfile.isBlank() ? "caption_rules_v1" : styleProfile.trim();
		return """
				TASK:
				Generate subtitle cues with timestamps for this audio chunk.

				TOON_CONTEXT (Token-Oriented Object Notation):
				// Compact, machine-oriented context for this request. Do not echo this block in your output.
				chunkIndex:i32 = %d
				chunkOffsetMs:i64 = %d
				chunkDurationMs:i64 = %d
				targetLanguage:str = %s
				styleProfile:str = %s

				OUTPUT LANGUAGE:
				- If targetLanguage is "my": output subtitle text in Myanmar (Burmese).
				- If the spoken audio is not Myanmar, translate to Myanmar while preserving meaning.
				- If targetLanguage is "original": output subtitle text in the same language as the spoken audio, and do NOT translate.
				- If targetLanguage is not "my" or "original": output subtitle text in that language (still accurate to the audio).

				CAPTION_RULES:
				- Keep captions short and readable.
				- Prefer 1–2 lines per cue.
				- Use natural expressive punctuation when it fits the audio (but do not add new meaning).
				- Do NOT change the meaning. Do NOT invent content.

				TIMING RULES:
				- Provide timestamps relative to the start of this chunk (0 ms).
				- Use integer milliseconds.
				- startMs < endMs, and both within [0, %d].
				- Create cues only when speech is present. Ignore background music/noise.

				OUTPUT FORMAT (STRICT):
				Return JSON only. No markdown. No code fences. No prose.
				The JSON must be an array of objects with exactly:
				  - startMs (int)
				  - endMs (int)
				  - text (string)

				Example:
				[
				  {"startMs": 1200, "endMs": 3400, "text": "…"},
				  {"startMs": 3600, "endMs": 5200, "text": "…"}
				]
				""".formatted(chunkIndex, chunkOffsetMs, chunkDurationMs, lang, style, chunkDurationMs);
	}

	@Deprecated
	public static String userPromptMyanmarToonCaptionRules(long chunkDurationMs) {
		return userPromptSubtitleCues(chunkDurationMs, 0L, 0, "my", "caption_rules_v1");
	}

	public static String extractJsonArray(String raw) {
		if (raw == null) return "[]";
		String s = raw.trim();
		int start = s.indexOf('[');
		int end = s.lastIndexOf(']');
		if (start >= 0 && end > start) {
			return s.substring(start, end + 1);
		}
		return "[]";
	}

	public static String userPromptRefineSrt(
			String srtText,
			String translatedText,
			String targetLanguage,
			String styleProfile
	) {
		String lang = targetLanguage == null || targetLanguage.isBlank() ? "my" : targetLanguage.trim();
		String style = styleProfile == null || styleProfile.isBlank() ? "caption_rules_v1" : styleProfile.trim();
		String safeSrt = srtText == null ? "" : srtText.trim();
		String safeTranslated = translatedText == null ? "" : translatedText.trim();
		return """
				TASK:
				Refine subtitle text using the translated script while preserving timing.

				TARGET_LANGUAGE:
				%s

				STYLE_PROFILE:
				%s

				STRICT RULES:
				- Keep the same number of cues as the input SRT.
				- Keep each cue index exactly as-is.
				- Keep each timestamp line exactly as-is. Do NOT modify start/end times.
				- Only rewrite subtitle text lines to match the translated script naturally.
				- Do NOT add, remove, merge, or split cues.
				- Output valid SRT only. No markdown. No code fences. No explanation.

				REFERENCE TRANSLATED SCRIPT:
				%s

				INPUT SRT TO REFINE:
				%s
				""".formatted(lang, style, safeTranslated, safeSrt);
	}

	public static String extractSrt(String raw) {
		if (raw == null) return "";
		String out = raw.trim();
		if (out.startsWith("```")) {
			out = out.replaceFirst("^```[a-zA-Z]*\\s*", "");
			out = out.replaceFirst("\\s*```\\s*$", "");
		}
		return out.trim();
	}
}

