package com.aiminion.aiservice.common.exception;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import com.aiminion.aiservice.common.response.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final int UPSTREAM_BODY_LOG_MAX = 4096;

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			fields.put(fe.getField(), fe.getDefaultMessage());
		}
		log.warn("Validation failed: {}", fields);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(400, "Validation failed", fields));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<?>> handleBadRequest(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(400, ex.getMessage(), null));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex) {
		log.warn("Illegal state: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.fail(503, ex.getMessage(), null));
	}

	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<ApiResponse<?>> handleRestClientResponse(RestClientResponseException ex) {
		String body = safeResponseBody(ex);
		String loggedBody = body.length() > UPSTREAM_BODY_LOG_MAX
				? body.substring(0, UPSTREAM_BODY_LOG_MAX) + "...(truncated)"
				: body;
		log.error("Upstream HTTP {} from RestClient — response body: {}", ex.getStatusCode().value(), loggedBody, ex);
		String message = "Upstream API HTTP " + ex.getStatusCode().value()
				+ (body.isBlank() ? "" : ": " + shortenForClient(body, 512));
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(ApiResponse.fail(502, message, null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleInternal(Exception ex) {
		log.error("Unhandled exception (returning generic 500)", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail(500, "Internal Server Error", null));
	}

	/**
	 * Handles AI content moderation violations.
	 * Returns HTTP 422 so frontend can properly display the error.
	 */
	@ExceptionHandler(ContentViolationException.class)
	public ResponseEntity<ApiResponse<?>> handleContentViolation(ContentViolationException ex) {
		log.warn("Content blocked: category={}, message={}",
				ex.getCategory(), ex.getMessage());

		Map<String, Object> details = new LinkedHashMap<>();
		details.put("category", ex.getCategory());

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ApiResponse.fail(
						HttpStatus.UNPROCESSABLE_ENTITY.value(),
						ex.getMessage(),
						details
				));
	}

	private static String safeResponseBody(RestClientResponseException ex) {
		try {
			String s = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
			return s != null ? s : "";
		} catch (Exception e) {
			return "";
		}
	}

	private static String shortenForClient(String body, int max) {
		String t = body.replaceAll("\\s+", " ").trim();
		return t.length() <= max ? t : t.substring(0, max) + "…";
	}
}