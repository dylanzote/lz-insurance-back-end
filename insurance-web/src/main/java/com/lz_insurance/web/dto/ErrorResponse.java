package com.lz_insurance.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Detailed error response for validation and exception handling.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String message;
    private Map<String, String> fieldErrors;
    private String path;
    private LocalDateTime timestamp;
    private String traceId;

    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static ErrorResponse of(String errorCode, String message, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .fieldErrors(fieldErrors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}

