package com.lz_insurance.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoverageDto {

    private String coverageId;
    private String coverageName;
    private String coverageType;
    private BigDecimal limit;
    private BigDecimal deductible;
    private boolean mandatory;
    private boolean optional;
    private Map<String, Object> parameters;
    private String description;
}
