package com.aiminion.aiservice.feature.response;

import lombok.Builder;

import java.util.List;

@Builder
public record VoiceOverProviderModels(String provider, List<VoiceModelDescriptor> models) {}
