package com.aiminion.aiservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.ai.google.genai.api-key=test-placeholder-key-for-context-load")
class AiserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
