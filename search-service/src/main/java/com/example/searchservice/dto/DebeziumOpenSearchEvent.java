package com.example.searchservice.dto;

import java.util.Map;

public record DebeziumOpenSearchEvent(
        String tableName,
        String operation,
        String indexName,
        String documentId,
        Map<String, Object> document
) {

    public boolean delete() {
        return "d".equals(operation);
    }
}
