package com.lz_insurance.product.engine.engine;

import com.lz_insurance.core.model.Money;
import com.lz_insurance.product.engine.model.Coverage;
import com.lz_insurance.product.engine.model.PremiumBasis;
import com.lz_insurance.product.engine.model.ProductSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingEngine {

    public Money calculatePremium(ProductSchema product, Map<String, Object> riskFactors) {
        PremiumBasis premiumBasis = product.getPremiumBasis();
        
        if (premiumBasis == null) {
            throw new IllegalArgumentException("Product must have premium basis defined");
        }

        BigDecimal basePremium = premiumBasis.getBaseRate();
        
        if (premiumBasis.getRatingFactors() != null) {
            for (Map.Entry<String, Object> entry : premiumBasis.getRatingFactors().entrySet()) {
                String factorExpression = entry.getValue().toString();
                try {
                    Object result = MVEL.eval(factorExpression, riskFactors);
                    if (result instanceof Number) {
                        BigDecimal factor = BigDecimal.valueOf(((Number) result).doubleValue());
                        basePremium = basePremium.multiply(factor);
                    }
                } catch (Exception e) {
                    log.warn("Failed to evaluate rating factor: {}", factorExpression, e);
                }
            }
        }

        basePremium = basePremium.setScale(2, RoundingMode.HALF_EVEN);
        
        return new Money(basePremium, Currency.getInstance("USD"));
    }

    public Money calculateCoveragePremium(Coverage coverage, BigDecimal basePremium, Map<String, Object> riskFactors) {
        BigDecimal coveragePremium = basePremium;
        
        if (coverage.getParameters() != null) {
            Object premiumMultiplier = coverage.getParameters().get("premiumMultiplier");
            if (premiumMultiplier instanceof Number) {
                coveragePremium = coveragePremium.multiply(
                    BigDecimal.valueOf(((Number) premiumMultiplier).doubleValue())
                );
            }
        }

        coveragePremium = coveragePremium.setScale(2, RoundingMode.HALF_EVEN);
        
        return new Money(coveragePremium, Currency.getInstance("USD"));
    }
}
