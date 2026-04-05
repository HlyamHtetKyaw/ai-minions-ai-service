package com.aiminion.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.google-ai")
public class GoogleAiProperties {
	private String transcribeModel = "gemini-flash-lite-latest";
}
