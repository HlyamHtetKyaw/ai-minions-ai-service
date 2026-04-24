package com.aiminion.aiservice.feature.response;

import lombok.Builder;

/**
 * TTS voice identifier (API value) and a short style label for UI.
 */
@Builder
public record VoiceModelDescriptor(String id, String style) {}
