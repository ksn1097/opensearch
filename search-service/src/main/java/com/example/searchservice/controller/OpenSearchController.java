package com.example.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.searchservice.dto.CreateIndexRequest;
import com.example.searchservice.dto.IndexRequest;
import com.example.searchservice.dto.SearchRequest;
import com.example.searchservice.dto.BulkIndexRequest;
import com.example.searchservice.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

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
    public ResponseEntity<Object> indexDocument(@Valid @RequestBody IndexRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildValidationErrorResponse(bindingResult));
        }
        return ResponseEntity.ok(service.indexDocument(request));
    }

    @Operation(summary = "Search documents", description = "Searches for documents in the specified OpenSearch index using the provided fields and values.")
    @PostMapping("/search")
    public ResponseEntity<Object> search(@Valid @RequestBody SearchRequest request, BindingResult bindingResult) {
        logger.info("Received search request: indexName='{}', fields={}", request.getIndexName(), request.getFields());
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildValidationErrorResponse(bindingResult));
        }
        return ResponseEntity.ok(service.search(request));
    }

    @Operation(summary = "Delete a document", description = "Deletes a document from the specified OpenSearch index by document ID.")
    @DeleteMapping("/documents/{indexName}/{documentId}")
    public ResponseEntity<Object> deleteDocument(
            @PathVariable String indexName,
            @PathVariable String documentId
    ) {
        return ResponseEntity.ok(service.deleteDocument(indexName, documentId));
    }

    @Operation(summary = "Create an index", description = "Creates a new OpenSearch index. If the index already exists, returns a message indicating so.")
    @PostMapping("/indexes")
    public ResponseEntity<Object> createIndex(@Valid @RequestBody CreateIndexRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildValidationErrorResponse(bindingResult));
        }
        return ResponseEntity.ok(service.createIndex(request.getIndexName()));
    }

    @Operation(summary = "Bulk index documents", description = "Indexes multiple documents into OpenSearch in a single request.")
    @PostMapping("/documents/bulk")
    public ResponseEntity<Object> bulkIndexDocuments(@Valid @RequestBody BulkIndexRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildValidationErrorResponse(bindingResult));
        }
        return ResponseEntity.ok(service.bulkIndexDocuments(request));
    }

    private Map<String, Object> buildValidationErrorResponse(BindingResult bindingResult) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", "VALIDATION_FAILED");
        response.put("errorCode", "VALIDATION_ERROR");
        Map<String, String> errors = new java.util.HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        response.put("errors", errors);
        return response;
    }
}
