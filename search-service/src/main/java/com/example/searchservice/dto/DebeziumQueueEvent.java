package com.example.searchservice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DebeziumQueueEvent {

    private String correlationId;
    private String dataRecordType;
    private String recordType;
    private Map<String, Object> payload;
    private String source;
}
