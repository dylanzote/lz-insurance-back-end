package com.lz_insurance.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Generic Errors (1000-1999)
    INTERNAL_SERVER_ERROR("INS-1000", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("INS-1001", "Validation failed", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("INS-1002", "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("INS-1003", "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("INS-1004", "Access forbidden", HttpStatus.FORBIDDEN),
    DUPLICATE_RESOURCE("INS-1005", "Resource already exists", HttpStatus.CONFLICT),
    BAD_REQUEST("INS-1006", "Bad request", HttpStatus.BAD_REQUEST),

    // Customer Service Errors (2000-2999)
    CUSTOMER_NOT_FOUND("INS-2000", "Customer not found", HttpStatus.NOT_FOUND),
    CUSTOMER_ALREADY_EXISTS("INS-2001", "Customer already exists", HttpStatus.CONFLICT),
    INVALID_KYC_DOCUMENT("INS-2002", "Invalid KYC document", HttpStatus.BAD_REQUEST),

    // Product Service Errors (3000-3999)
    PRODUCT_NOT_FOUND("INS-3000", "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_VERSION_CONFLICT("INS-3001", "Product version conflict", HttpStatus.CONFLICT),
    INVALID_PRODUCT_SCHEMA("INS-3002", "Invalid product schema", HttpStatus.BAD_REQUEST),
    PRODUCT_EXPIRED("INS-3003", "Product is expired", HttpStatus.BAD_REQUEST),

    // Policy Service Errors (4000-4999)
    POLICY_NOT_FOUND("INS-4000", "Policy not found", HttpStatus.NOT_FOUND),
    POLICY_INVALID_STATUS_TRANSITION("INS-4001", "Invalid policy status transition", HttpStatus.BAD_REQUEST),
    POLICY_EXPIRED("INS-4002", "Policy has expired", HttpStatus.BAD_REQUEST),
    POLICY_ALREADY_CANCELLED("INS-4003", "Policy already cancelled", HttpStatus.BAD_REQUEST),

    // Claims Service Errors (5000-5999)
    CLAIM_NOT_FOUND("INS-5000", "Claim not found", HttpStatus.NOT_FOUND),
    CLAIM_INVALID_STATUS_TRANSITION("INS-5001", "Invalid claim status transition", HttpStatus.BAD_REQUEST),
    CLAIM_ALREADY_SETTLED("INS-5002", "Claim already settled", HttpStatus.BAD_REQUEST),

    // Payment Errors (6000-6999)
    PAYMENT_FAILED("INS-6000", "Payment processing failed", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS("INS-6001", "Insufficient funds", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND("INS-6002", "Payment not found", HttpStatus.NOT_FOUND),

    // Storage Errors (7000-7999)
    STORAGE_ERROR("INS-7000", "Storage operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_NOT_FOUND("INS-7001", "Document not found", HttpStatus.NOT_FOUND),

    // Messaging Errors (8000-8999)
    KAFKA_ERROR("INS-8000", "Kafka messaging error", HttpStatus.INTERNAL_SERVER_ERROR),
    EVENT_PROCESSING_FAILED("INS-8001", "Event processing failed", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
