package com.aiminion.aiservice.common.ai.context;

/**
 * Per-request override for Gemini REST / GenAI calls (set from {@code X-User-Gemini-Api-Key}).
 */
public final class UserGeminiApiKeyContext {

	private static final ThreadLocal<String> KEY = new ThreadLocal<>();

	private UserGeminiApiKeyContext() {
	}

	public static void set(String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			KEY.remove();
		} else {
			KEY.set(apiKey.trim());
		}
	}

	public static String get() {
		return KEY.get();
	}

	public static void clear() {
		KEY.remove();
	}
}
