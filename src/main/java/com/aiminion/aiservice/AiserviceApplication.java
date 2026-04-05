package com.aiminion.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.aiminion.aiservice.config.GoogleAiProperties;

@SpringBootApplication
@EnableConfigurationProperties({ GoogleAiProperties.class })
public class AiserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiserviceApplication.class, args);
	}

}
