package com.example.searchservice.service;

import com.example.searchservice.config.DebeziumIngestionProperties;
import com.example.searchservice.dto.DebeziumOpenSearchEvent;
import com.example.searchservice.dto.DebeziumQueueEvent;
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

    private final ObjectMapper objectMapper;
    private final DebeziumIngestionProperties properties;
    private final IdentityDocumentTransformer identityDocumentTransformer;

    public Optional<DebeziumOpenSearchEvent> parse(String rawMessage) {
        try {
            JsonNode root = unwrapSnsMessage(objectMapper.readTree(rawMessage));
            DebeziumQueueEvent queueEvent = objectMapper.convertValue(root, DebeziumQueueEvent.class);
            JsonNode payload = root.path("payload");

            if (queueEvent.getPayload() == null || payload.isMissingNode() || payload.isNull()) {
                return Optional.empty();
            }

            String dataRecordType = resolveDataRecordType(root, payload, queueEvent);
            String indexName = resolveIndexName(dataRecordType);

            if (dataRecordType == null || indexName == null) {
                return Optional.empty();
            }

            JsonNode row = payload;

            if (row.isMissingNode() || row.isNull()) {
                return Optional.empty();
            }

            String documentId = resolveDocumentId(dataRecordType, row);
            Map<String, Object> rowPayload = queueEvent.getPayload();
            Map<String, Object> document = identityDocumentTransformer.transform(rowPayload);

            return Optional.of(new DebeziumOpenSearchEvent(dataRecordType, indexName, documentId, document));
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

    private String resolveDataRecordType(JsonNode root, JsonNode payload, DebeziumQueueEvent queueEvent) {
        return firstText(
                root.path("dataRecordType"),
                payload.path("dataRecordType")
        );
    }

    private String resolveIndexName(String dataRecordType) {
        if (dataRecordType == null) {
            return null;
        }

        return properties.dataRecordTypeIndexMap().get(dataRecordType);
    }

    private String resolveDocumentId(String dataRecordType, JsonNode row) {
        List<String> idFields = properties.dataRecordTypeIdFields().get(dataRecordType);

        if (idFields == null || idFields.isEmpty()) {
            throw new IllegalArgumentException("No document id fields configured for dataRecordType " + dataRecordType);
        }

        for (String idField : idFields) {
            JsonNode idValue = row.path(idField);

            if (!idValue.isMissingNode() && !idValue.isNull() && !idValue.asText().isBlank()) {
                return idValue.asText();
            }
        }

        throw new IllegalArgumentException("Unable to resolve document id for dataRecordType " + dataRecordType);
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
