package com.lz_insurance.dto.claim;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ClaimDto {

    private String claimId;
    private String claimNumber;
    private String policyId;
    private String policyNumber;
    private String customerId;
    private String status;
    private String claimType;
    private Money reserveAmount;
    private Money paidAmount;
    private Money estimatedLoss;
    private LocalDateTime incidentDate;
    private LocalDateTime reportedDate;
    private LocalDateTime closedDate;
    private String assignedAdjuster;
    private List<ClaimDocumentDto> documents;
    private Map<String, Object> claimDetails;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
