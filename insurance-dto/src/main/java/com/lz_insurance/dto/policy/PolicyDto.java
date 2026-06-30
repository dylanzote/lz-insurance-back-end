package com.lz_insurance.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lz_insurance.core.model.Address;
import com.lz_insurance.core.model.DateRange;
import com.lz_insurance.core.model.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyDto {

    private String policyId;
    private String policyNumber;
    private String productId;
    private String customerId;
    private String status;
    private DateRange policyPeriod;
    private Money totalPremium;
    private Money netPremium;
    private Money tax;
    private List<PolicyCoverageDto> coverages;
    private Address riskAddress;
    private Map<String, Object> riskData;
    private LocalDateTime issueDate;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
