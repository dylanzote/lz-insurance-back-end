package com.lz_Insurance.product.engine.model;

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
public class PremiumBasis {

    private String basisType;
    private String calculationMethod;
    private BigDecimal baseRate;
    private String frequency;
    private Map<String, Object> ratingFactors;
    private Map<String, Object> formulas;
}
