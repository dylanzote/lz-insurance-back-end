package com.lz_Insurance.web.advice;

import com.lz_Insurance.core.exception.ErrorCode;
import com.lz_Insurance.core.exception.InsuranceException;
import com.lz_Insurance.core.exception.ValidationException;
import com.lz_Insurance.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across all services.
 */
@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(InsuranceException.class)
    public ResponseEntity<ErrorResponse> handleInsuranceException(InsuranceException ex, HttpServletRequest request) {

        log.error("Insurance exception occurred: {}", ex.getFormattedMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode().getCode())
                .message(ex.getFormattedMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {

        log.warn("Validation exception: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ex.getMessage())
                .fieldErrors(ex.getFieldErrors())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ?
                                error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Validation failed for one or more fields")
                .fieldErrors(fieldErrors)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("An unexpected error occurred. Please contact support.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}