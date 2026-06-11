package com.lz_Insurance.core.exception;

public class ResourceNotFoundException extends InsuranceException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
              String.format("%s with identifier '%s' not found", resourceType, identifier));
    }

    public ResourceNotFoundException(String resourceType, String fieldName, Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
              String.format("%s with %s '%s' not found", resourceType, fieldName, value));
    }

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
