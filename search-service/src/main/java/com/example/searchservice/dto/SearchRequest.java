package com.example.searchservice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class SearchRequest {

    @NotBlank(message = "indexName must not be blank")
    private String indexName;
    @NotNull(message = "fields must not be null")
    @Size(min = 1, message = "fields must not be empty")
    private Map<String, String> fields;
}
