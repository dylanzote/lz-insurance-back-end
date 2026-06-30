package com.lz_insurance.product.engine.validator;

import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.product.engine.exception.SchemaValidationException;
import com.lz_insurance.product.engine.model.Condition;
import com.lz_insurance.product.engine.model.Coverage;
import com.lz_insurance.product.engine.model.ProductSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoverageValidator {

    public void validate(ProductSchema product, Map<String, Object> riskData) {
        if (product.getConditions() != null) {
            for (Condition condition : product.getConditions()) {
                if (!evaluateCondition(condition, riskData)) {
                    String errorMessage = condition.getErrorMessage() != null
                        ? condition.getErrorMessage()
                        : "Condition validation failed: " + condition.getConditionName();
                    throw new SchemaValidationException(ErrorCode.VALIDATION_FAILED, errorMessage);
                }
            }
        }
    }

    public void validateCoverageSelection(List<String> selectedCoverageIds, ProductSchema product) {
        for (String coverageId : selectedCoverageIds) {
            Coverage coverage = findCoverage(coverageId, product.getCoverages());
            if (coverage == null) {
                throw new SchemaValidationException(ErrorCode.VALIDATION_FAILED, "Coverage not found: " + coverageId);
            }

            if (coverage.getDependencies() != null) {
                for (String dependency : coverage.getDependencies()) {
                    if (!selectedCoverageIds.contains(dependency)) {
                        throw new SchemaValidationException(
                            ErrorCode.VALIDATION_FAILED,
                            "Coverage " + coverageId + " requires dependency: " + dependency
                        );
                    }
                }
            }
        }

        for (Coverage coverage : product.getCoverages()) {
            if (coverage.isMandatory() && !selectedCoverageIds.contains(coverage.getCoverageId())) {
                throw new SchemaValidationException(
                    ErrorCode.VALIDATION_FAILED,
                    "Mandatory coverage missing: " + coverage.getCoverageId()
                );
            }
        }
    }

    private boolean evaluateCondition(Condition condition, Map<String, Object> riskData) {
        try {
            if (condition.getExpression() != null) {
                Object result = MVEL.eval(condition.getExpression(), riskData);
                return Boolean.TRUE.equals(result);
            }
            return true;
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition.getConditionName(), e);
            return false;
        }
    }

    private Coverage findCoverage(String coverageId, List<Coverage> coverages) {
        return coverages.stream()
            .filter(c -> c.getCoverageId().equals(coverageId))
            .findFirst()
            .orElse(null);
    }
}
