package com.lz_insurance.security.core.spi;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Map<String, Object> attributes;
    private Boolean enabled;
}
