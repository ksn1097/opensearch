package com.example.searchservice.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
@Getter
@Setter
public class IndexRequest {

    @NotBlank(message = "indexName must not be blank")
    private String indexName;
    @NotBlank(message = "documentId must not be blank")
    private String documentId;
    @NotNull(message = "document must not be null")
    @Size(min = 1, message = "document must not be empty")
    private Map<String, Object> document;
}
