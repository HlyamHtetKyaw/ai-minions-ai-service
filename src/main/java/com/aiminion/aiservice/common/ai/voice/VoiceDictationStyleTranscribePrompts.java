package com.aiminion.aiservice.common.ai.voice;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class VoiceDictationStyleTranscribePrompts {

	private static final String BASE_RULES = """
			RULES:
			1. AUDIO ANALYSIS: If the audio contains ONLY music, noise, or silence with no clear speech, return:
			   Title: No Speech Detected
			   Content: [No speech detected in this audio segment]

			CRITICAL LANGUAGE RULES:
			       1. STRICTLY PRESERVE the original language of the input audio.
			       2. If the audio is Burmese, the summary MUST be in Burmese.
			       3. If the audio is English, the summary MUST be in English.
			       4. If the audio is mixed (Burmese + English), the summary MUST maintain that bilingual mix naturally.
			       5. DO NOT TRANSLATE the content into a different language.
			       6. IMPORTANT: Do NOT "helpfully" translate English into Burmese or Burmese into English. Keep the exact language(s) you hear.

			2. IGNORE BACKGROUND: Do not transcribe lyrics of background songs or repetitive noises. Only transcribe clear spoken dialogue.
			3. LOOP SAFETY: Do not repeat the same phrase more than twice. If you are unsure of a word, skip it.
			4. FAILURE SAFETY: If the audio is completely unintelligible, do not output random words.
			5. NEVER repeat words or phrases.
			6. NEVER guess missing content.
			7. STOP immediately if meaning cannot be determined.
			""";

	private static final String PLAIN_TRANSCRIBE_SUFFIX = """
			Transcribe the spoken audio exactly word-for-word in its original language(s). If the audio is English-only, output English-only. If Burmese-only, output Burmese-only. If mixed, keep it mixed. Do not summarize or paraphrase.
			Output plain text only (no Speaker labels).
			Do not add timestamps.
			CRITICAL: If English vocabulary words (like 'Platinum', 'Member', 'Premium', 'Gold', 'Silver', etc.) are spoken,
			PRESERVE them in English - DO NOT transliterate to Burmese.
			Example: 'ငါက Platinum Member ဖြစ်တယ်' (NOT 'ငါက ပလက်တီတန် မန်ဘာ ဖြစ်တယ်').
			Keep English words in their original English form when they appear in the speech.
			""";

	private static final String FORMATTING_INSTRUCTION = """

			IMPORTANT OUTPUT FORMAT:
			You must respond in this exact format:
			Title: [Generate a short, relevant title in the SAME LANGUAGE as the input audio]
			Content:
			[If task is TRANSCRIBE (no speakerAnalyze): output plain text only, no Speaker labels.]
			[If task is TRANSCRIBE + speakerAnalyze AND multiple speakers: use Speaker 1..Speaker N labels when speakers change.]
			[If task is SUMMARIZE: write a cohesive summary, no Speaker labels.]
			""";

	public static String fullUserTextPrompt() {
		return BASE_RULES + PLAIN_TRANSCRIBE_SUFFIX + FORMATTING_INSTRUCTION;
	}

	public static String extractContent(String rawText) {
		if (rawText == null) {
			return "";
		}
		var content = rawText;
		if (rawText.contains("Content:")) {
			String[] parts = rawText.split("Content:", 2);
			if (parts.length > 1) {
				content = parts[1].trim();
			}
		}
		return content.trim();
	}
}
