package com.aiminion.aiservice.common.response.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMeta {
    private long totalItems;
    private int totalPages;
    private int currentPage;
    private String method;
    private String endpoint;
}
