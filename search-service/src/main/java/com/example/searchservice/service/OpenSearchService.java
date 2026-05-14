package com.example.searchservice.service;

import com.example.searchservice.dto.BulkIndexRequest;
import com.example.searchservice.dto.IndexRequest;
import com.example.searchservice.dto.SearchRequest;
import com.example.searchservice.exception.OpenSearchServiceException;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch._types.FieldValue;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenSearchService {

    private final OpenSearchClient client;

    @Retryable(
            retryFor = OpenSearchServiceException.class,
            backoff = @Backoff(delay = 2000)
    )
    public String indexDocument(IndexRequest request) {
        try {
            IndexResponse response = client.index(i -> i
                    .index(request.getIndexName())
                    .id(request.getDocumentId())
                    .document(request.getDocument())
            );
            return response.result().jsonValue();
        } catch (Exception e) {
            throw new OpenSearchServiceException("Failed to index document", e);
        }
    }

    public List<Map<String, Object>> search(SearchRequest request) {
        try {
            SearchResponse<Map<String, Object>> response = client.search(s -> s
                .index(request.getIndexName())
                .query(q -> q
                    .bool(b -> {
                        for (Map.Entry<String, String> entry : request.getFields().entrySet()) {
                            b.must(mustQuery -> mustQuery
                                .match(m -> m
                                    .field(entry.getKey())
                                    .query(FieldValue.of(entry.getValue()))
                                )
                            );
                        }
                        return b;
                    })
                ),
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map<String, Object>> hit : response.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new OpenSearchServiceException("Failed to search documents", e);
        }
    }

    public String deleteDocument(String indexName, String documentId) {
        try {
            DeleteResponse response = client.delete(d -> d
                    .index(indexName)
                    .id(documentId)
            );
            return response.result().jsonValue();
        } catch (Exception e) {
            throw new OpenSearchServiceException("Failed to delete document", e);
        }
    }

    public String createIndex(String indexName) {
        try {
            org.opensearch.client.transport.endpoints.BooleanResponse existsResponse = client.indices().exists(e -> e.index(indexName));
            if (existsResponse.value()) {
                return "index already present";
            }
            // Create index
            org.opensearch.client.opensearch.indices.CreateIndexResponse createResponse = client.indices().create(c -> c.index(indexName));
            if (Boolean.TRUE.equals(createResponse.acknowledged())) {
                return "content created";
            } else {
                throw new OpenSearchServiceException("Index creation not acknowledged");
            }
        } catch (Exception e) {
            throw new OpenSearchServiceException("Failed to create index", e);
        }
    }

    public String bulkIndexDocuments(BulkIndexRequest request) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (IndexRequest docReq : request.getDocuments()) {
                br.operations(op -> op
                    .index(idx -> idx
                        .index(docReq.getIndexName())
                        .id(docReq.getDocumentId())
                        .document(docReq.getDocument())
                    )
                );
            }
            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                StringBuilder errorMsg = new StringBuilder("Bulk index had errors: ");
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        errorMsg.append("[index: ").append(item.index()).append(", id: ").append(item.id()).append(", error: ").append(item.error().reason()).append("] ");
                    }
                }
                throw new OpenSearchServiceException(errorMsg.toString());
            }
            return "Bulk indexing successful: " + response.items().size() + " documents indexed.";
        } catch (Exception e) {
            throw new OpenSearchServiceException("Failed to bulk index documents", e);
        }
    }
}
