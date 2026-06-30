package com.lz_insurance.product.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSchema {

    private String productId;
    private String productName;
    private String productType;
    private String version;
    private String description;
    private List<Coverage> coverages;
    private List<Condition> conditions;
    private PremiumBasis premiumBasis;
    private Map<String, Object> rules;
    private Map<String, Object> metadata;
    private boolean active;
}
