package com.aiminion.aiservice.common.response.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Jacksonized
@Builder
public class PaginatedApiResponse<T> {
	private int success;
    private int code;
    private String message;
    private PaginationMeta meta;
    private List<T> data;
}
