package com.example.searchservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class BulkIndexRequest {
    @NotEmpty(message = "documents list must not be empty")
    @Valid
    private List<IndexRequest> documents;
}
