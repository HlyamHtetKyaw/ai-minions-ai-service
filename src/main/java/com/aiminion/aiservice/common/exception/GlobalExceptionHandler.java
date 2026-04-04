package com.aiminion.aiservice.common.exception;

import com.aiminion.aiservice.common.response.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.fail(404, ex.getMessage(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, Object> fields = new LinkedHashMap<>();

		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			fields.put(fe.getField(), fe.getDefaultMessage());
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(400, "Validation failed", fields));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<?>> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(400, ex.getMessage(), null));
	}

	@ExceptionHandler(Exception.class) // 🔥 better than RuntimeException
	public ResponseEntity<ApiResponse<?>> handleInternal(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail(500, "Internal Server Error", null));
	}
}