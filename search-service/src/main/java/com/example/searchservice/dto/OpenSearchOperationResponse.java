package com.example.searchservice.dto;

public record OpenSearchOperationResponse(
        String result,
        String indexName,
        String documentId,
        Integer documentCount
) {
}
