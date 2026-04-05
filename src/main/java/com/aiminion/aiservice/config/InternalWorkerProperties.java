package com.aiminion.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.internal")
public class InternalWorkerProperties {
	private String workerToken = "";

	public boolean isWorkerAuthEnabled() {
		return workerToken != null && !workerToken.isBlank();
	}
}
