package com.aiminion.aiservice.common.ai.context;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads optional {@value #HEADER_NAME} and exposes it to Gemini clients for this request only.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserGeminiApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-User-Gemini-Api-Key";

	@Override
	protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			String raw = request.getHeader(HEADER_NAME);
			if (raw != null && !raw.isBlank()) {
				System.out.println("Setting user Gemini API key for request: " + raw);
				UserGeminiApiKeyContext.set(raw);
			} else {
				System.out.println("No user Gemini API key found for request");
			}
			filterChain.doFilter(request, response);
		} finally {
			UserGeminiApiKeyContext.clear();
		}
	}
}
