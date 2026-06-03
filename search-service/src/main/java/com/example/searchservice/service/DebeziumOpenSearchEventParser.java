package com.example.searchservice.service;

import com.example.searchservice.config.DebeziumIngestionProperties;
import com.example.searchservice.dto.DebeziumOpenSearchEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DebeziumOpenSearchEventParser {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final DebeziumIngestionProperties properties;
    private final IdentityDocumentTransformer identityDocumentTransformer;

    public Optional<DebeziumOpenSearchEvent> parse(String rawMessage) {
        try {
            JsonNode root = unwrapSnsMessage(objectMapper.readTree(rawMessage));
            JsonNode payload = root.path("payload");

            if (payload.isMissingNode() || payload.isNull()) {
                return Optional.empty();
            }

            String operation = payload.path("op").asText(null);
            String tableName = resolveTableName(root, payload);
            String indexName = properties.tableIndexMap().get(tableName);

            if (tableName == null || indexName == null) {
                return Optional.empty();
            }

            JsonNode row = resolveRecordPayload(payload);

            if (row.isMissingNode() || row.isNull()) {
                return Optional.empty();
            }

            String documentId = resolveDocumentId(tableName, row);
            Map<String, Object> rowPayload = objectMapper.convertValue(row, MAP_TYPE);
            Map<String, Object> document = "d".equals(operation)
                    ? Map.of()
                    : identityDocumentTransformer.transform(rowPayload);

            return Optional.of(new DebeziumOpenSearchEvent(tableName, operation, indexName, documentId, document));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Debezium OpenSearch event", e);
        }
    }

    private JsonNode unwrapSnsMessage(JsonNode root) throws Exception {
        JsonNode message = root.path("Message");

        if (message.isMissingNode() || message.isNull()) {
            return root;
        }

        return objectMapper.readTree(message.asText());
    }

    private String resolveTableName(JsonNode root, JsonNode payload) {
        String tableName = firstText(
                payload.path("tableName"),
                payload.path("table"),
                payload.path("source").path("table"),
                root.path("tableName"),
                root.path("table")
        );

        if (tableName == null) {
            return properties.defaultTableName();
        }

        return tableName;
    }

    private JsonNode resolveRecordPayload(JsonNode payload) {
        for (String fieldName : List.of("record", "data", "document", "after")) {
            JsonNode candidate = payload.path(fieldName);

            if (!candidate.isMissingNode() && !candidate.isNull()) {
                return candidate;
            }
        }

        return payload;
    }

    private String resolveDocumentId(String tableName, JsonNode row) {
        List<String> idFields = properties.tableIdFields().getOrDefault(tableName, properties.defaultIdFields());

        for (String idField : idFields) {
            JsonNode idValue = row.path(idField);

            if (!idValue.isMissingNode() && !idValue.isNull() && !idValue.asText().isBlank()) {
                return idValue.asText();
            }
        }

        throw new IllegalArgumentException("Unable to resolve document id for table " + tableName);
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (!node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }

        return null;
    }
}
