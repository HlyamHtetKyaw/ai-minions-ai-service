package com.aiminion.aiservice.feature.response;

import lombok.Builder;

import java.util.List;

@Builder
public record VoiceOverModelsResponse(List<VoiceOverProviderModels> providers) {}
