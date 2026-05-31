package com.example.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.searchservice.dto.ApiResponse;
import com.example.searchservice.dto.CreateIndexRequest;
import com.example.searchservice.dto.IndexRequest;
import com.example.searchservice.dto.OpenSearchOperationResponse;
import com.example.searchservice.dto.SearchRequest;
import com.example.searchservice.dto.BulkIndexRequest;
import com.example.searchservice.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "OpenSearch Controller", description = "APIs for interacting with OpenSearch indices and documents.")
public class OpenSearchController {

    private final OpenSearchService service;
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchController.class);

    @Operation(summary = "Index a document", description = "Indexes a document into the specified OpenSearch index.")
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<OpenSearchOperationResponse>> indexDocument(@Valid @RequestBody IndexRequest request) {
        OpenSearchOperationResponse response = service.indexDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document indexed successfully", response));
    }

    @Operation(summary = "Search documents", description = "Searches for documents in the specified OpenSearch index using the provided fields and values.")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(@Valid @RequestBody SearchRequest request) {
        logger.info("Received search request: indexName='{}', fields={}", request.getIndexName(), request.getFields());
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", service.search(request)));
    }

    @Operation(summary = "Delete a document", description = "Deletes a document from the specified OpenSearch index by document ID.")
    @DeleteMapping("/documents/{indexName}/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String indexName,
            @PathVariable String documentId
    ) {
        service.deleteDocument(indexName, documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create an index", description = "Creates a new OpenSearch index. If the index already exists, returns a message indicating so.")
    @PostMapping("/indexes")
    public ResponseEntity<ApiResponse<OpenSearchOperationResponse>> createIndex(@Valid @RequestBody CreateIndexRequest request) {
        OpenSearchOperationResponse response = service.createIndex(request.getIndexName());
        HttpStatus status = "created".equals(response.result()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Index request completed successfully", response));
    }

    @Operation(summary = "Bulk index documents", description = "Indexes multiple documents into OpenSearch in a single request.")
    @PostMapping("/documents/bulk")
    public ResponseEntity<ApiResponse<OpenSearchOperationResponse>> bulkIndexDocuments(@Valid @RequestBody BulkIndexRequest request) {
        OpenSearchOperationResponse response = service.bulkIndexDocuments(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk indexing completed successfully", response));
    }
}
