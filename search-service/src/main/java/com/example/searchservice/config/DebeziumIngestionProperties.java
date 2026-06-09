package com.example.searchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.debezium-ingestion")
public record DebeziumIngestionProperties(
        Map<String, String> dataRecordTypeIndexMap,
        Map<String, List<String>> dataRecordTypeIdFields
) {

    public DebeziumIngestionProperties {
        dataRecordTypeIndexMap = dataRecordTypeIndexMap == null ? Map.of() : Map.copyOf(dataRecordTypeIndexMap);
        dataRecordTypeIdFields = dataRecordTypeIdFields == null ? Map.of() : Map.copyOf(dataRecordTypeIdFields);
    }
}
