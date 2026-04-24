package com.aiminion.aiservice.common.sanitizer.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parsed verdict returned by the AI sanitization check.
 *
 * @param safe     true  → content is clean, proceed normally
 *                 false → content is harmful/policy-violating, reject
 * @param reason   short human-readable explanation (always present)
 * @param category violation category, e.g. "HATE_SPEECH", "PII", "NONE"
 */
public record SanitizationResult(
        @JsonProperty("safe")     boolean safe,
        @JsonProperty("reason")   String  reason,
        @JsonProperty("category") String  category
) {}