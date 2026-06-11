package com.lz_Insurance.product.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Coverage {

    private String coverageId;
    private String coverageName;
    private String coverageType;
    private BigDecimal limit;
    private BigDecimal deductible;
    private boolean mandatory;
    private boolean optional;
    private List<String> dependencies;
    private Map<String, Object> parameters;
    private String description;
}
