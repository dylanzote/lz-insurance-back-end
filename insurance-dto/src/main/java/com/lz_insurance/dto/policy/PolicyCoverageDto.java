package com.lz_insurance.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lz_insurance.core.model.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyCoverageDto {

    private String coverageId;
    private String coverageName;
    private Money limit;
    private Money deductible;
    private Money premium;
    private boolean selected;
    private Map<String, Object> parameters;
}
