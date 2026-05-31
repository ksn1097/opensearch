package com.example.searchservice.service;

import com.example.searchservice.dto.BulkIndexRequest;
import com.example.searchservice.dto.DebeziumOpenSearchEvent;
import com.example.searchservice.dto.IndexRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebeziumOpenSearchIngestionService {

    private final DebeziumOpenSearchEventParser parser;
    private final OpenSearchService openSearchService;

    public void process(List<String> rawMessages) {
        List<IndexRequest> indexRequests = new ArrayList<>();
        int skipped = 0;

        for (String rawMessage : rawMessages) {
            DebeziumOpenSearchEvent event = parser.parse(rawMessage).orElse(null);

            if (event == null) {
                skipped++;
                continue;
            }

            indexRequests.add(toIndexRequest(event));
        }

        if (!indexRequests.isEmpty()) {
            BulkIndexRequest bulkIndexRequest = new BulkIndexRequest();
            bulkIndexRequest.setDocuments(indexRequests);
            openSearchService.bulkIndexDocuments(bulkIndexRequest);
        }

        log.info(
                "Processed Debezium SQS batch: indexed={}, skipped={}",
                indexRequests.size(),
                skipped
        );
    }

    private IndexRequest toIndexRequest(DebeziumOpenSearchEvent event) {
        IndexRequest request = new IndexRequest();
        request.setIndexName(event.indexName());
        request.setDocumentId(event.documentId());
        request.setDocument(event.document());
        return request;
    }
}
