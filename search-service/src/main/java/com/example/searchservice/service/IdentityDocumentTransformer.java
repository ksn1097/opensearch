package com.example.searchservice.service;

import com.example.searchservice.dto.IdentityPayloadDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IdentityDocumentTransformer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public Map<String, Object> transform(Map<String, Object> payload) {
        IdentityPayloadDto identityPayload = objectMapper.convertValue(payload, IdentityPayloadDto.class);
        Map<String, Object> document = objectMapper.convertValue(identityPayload, MAP_TYPE);
        document.values().removeIf(Objects::isNull);
        return document;
    }
}
