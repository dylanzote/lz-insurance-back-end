package com.lz_insurance.product.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class Condition {

    private String conditionId;
    private String conditionName;
    private String conditionType;
    private String expression;
    private String errorMessage;
    private boolean mandatory;
    private Map<String, Object> parameters;
}
