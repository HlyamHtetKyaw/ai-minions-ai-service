package com.aiminion.aiservice.common.request;

import lombok.Builder;

@Builder
public record PaginationRequest(
        String keyword,
        int page,
        int size,
        String sortField,
        String sortDirection
) {}
