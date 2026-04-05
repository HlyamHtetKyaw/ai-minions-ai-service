package com.aiminion.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.aiminion.aiservice.config.GoogleAiProperties;
import com.aiminion.aiservice.config.InternalWorkerProperties;

@SpringBootApplication
@EnableConfigurationProperties({ GoogleAiProperties.class, InternalWorkerProperties.class })
public class AiserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiserviceApplication.class, args);
	}

}
