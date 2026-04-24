package com.aiminion.aiservice.common.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)  // 422 — semantically wrong, not malformed
public class ContentViolationException extends RuntimeException {

    private final String category;

    public ContentViolationException(String category, String reason) {
        super(String.format("[%s] %s", category, reason));
        this.category = category;
    }

    public String getCategory() { return category; }
}