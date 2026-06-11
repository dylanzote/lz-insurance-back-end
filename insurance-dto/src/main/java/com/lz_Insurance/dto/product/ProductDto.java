package com.lz_Insurance.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ProductDto {

    private String productId;
    private String productName;
    private String productType;
    private String version;
    private String description;
    private List<CoverageDto> coverages;
    private Map<String, Object> metadata;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
