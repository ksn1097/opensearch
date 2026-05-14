package com.example.searchservice.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Data
@Getter
@Setter
public class CreateIndexRequest {
    @NotBlank(message = "indexName must not be blank")
    private String indexName;
}
