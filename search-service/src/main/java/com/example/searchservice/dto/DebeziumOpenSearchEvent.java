package com.example.searchservice.dto;

import java.util.Map;

public record DebeziumOpenSearchEvent(
        String tableName,
        String indexName,
        String documentId,
        Map<String, Object> document
) {
}
