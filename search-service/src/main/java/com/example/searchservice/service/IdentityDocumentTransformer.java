package com.example.searchservice.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IdentityDocumentTransformer {

    public Map<String, Object> transform(Map<String, Object> payload) {
        // TODO: Replace this temporary identity document shape once the OpenSearch mapping is confirmed.
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("identity", payload);
        return document;
    }
}
