package com.example.searchservice.exception;

public class OpenSearchServiceException extends RuntimeException {
    public OpenSearchServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    public OpenSearchServiceException(String message) {
        super(message);
    }
}
