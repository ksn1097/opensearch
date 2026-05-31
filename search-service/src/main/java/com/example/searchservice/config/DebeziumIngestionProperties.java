package com.example.searchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.debezium-ingestion")
public record DebeziumIngestionProperties(
        Map<String, String> tableIndexMap,
        Map<String, List<String>> tableIdFields,
        List<String> defaultIdFields
) {

    public DebeziumIngestionProperties {
        tableIndexMap = tableIndexMap == null ? Map.of() : Map.copyOf(tableIndexMap);
        tableIdFields = tableIdFields == null ? Map.of() : Map.copyOf(tableIdFields);
        defaultIdFields = defaultIdFields == null ? List.of("id", "identity_id") : List.copyOf(defaultIdFields);
    }
}
