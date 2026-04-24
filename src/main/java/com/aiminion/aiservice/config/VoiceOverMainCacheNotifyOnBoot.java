package com.aiminion.aiservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * When configured, notifies the main service after AI startup so it drops any stale Redis
 * cache for the voice-over TTS model catalog. Uses the same {@code X-Worker-Token} contract
 * as other internal worker endpoints on main.
 */
@Slf4j
@Component
public class VoiceOverMainCacheNotifyOnBoot implements ApplicationListener<ApplicationReadyEvent> {

	private static final String WORKER_TOKEN_HEADER = "X-Worker-Token";

	@Value("${voice-over.main-service.cache-invalidate-url:}")
	private String invalidateUrl;

	@Value("${voice-over.main-service.worker-token:}")
	private String workerToken;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (!StringUtils.hasText(invalidateUrl)) {
			return;
		}
		if (!StringUtils.hasText(workerToken)) {
			log.warn(
					"voice-over.main-service.cache-invalidate-url is set but worker-token is blank; skipping main cache invalidation.");
			return;
		}
		try {
			RestClient.create()
					.post()
					.uri(invalidateUrl.trim())
					.header(WORKER_TOKEN_HEADER, workerToken.trim())
					.retrieve()
					.toBodilessEntity();
			log.info("Posted to main to invalidate voice-over models Redis cache after AI startup.");
		} catch (Exception e) {
			log.warn("Could not invalidate main voice-over models cache ({}): {}", invalidateUrl, e.getMessage());
		}
	}
}
