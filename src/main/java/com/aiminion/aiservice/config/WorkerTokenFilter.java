package com.aiminion.aiservice.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkerTokenFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Worker-Token";

	private final InternalWorkerProperties internalWorkerProperties;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!internalWorkerProperties.isWorkerAuthEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}
		String path = request.getRequestURI();
		if (!path.contains("/feature/generate")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = request.getHeader(HEADER);
		if (token == null || !internalWorkerProperties.getWorkerToken().equals(token.trim())) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType("application/json");
			response.getWriter().write("{\"message\":\"Invalid or missing worker token\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
