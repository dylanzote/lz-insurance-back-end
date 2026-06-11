package com.lz_Insurance.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lz_Insurance.core.model.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto {

    private String customerId;
    private String customerNumber;
    private String customerType;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private Address primaryAddress;
    private Address mailingAddress;
    private String taxId;
    private List<String> roles;
    private Map<String, Object> attributes;
    private boolean emailVerified;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
