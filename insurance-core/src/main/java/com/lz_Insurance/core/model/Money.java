package com.lz_Insurance.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

@Data
@NoArgsConstructor
public class Money implements Serializable, Comparable<Money> {

    private BigDecimal amount;

    private Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        this.currency = currency;
    }

    public Money(BigDecimal amount, String currencyCode) {
        this(amount, Currency.getInstance(currencyCode));
    }

    public Money(double amount, Currency currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), currencyCode);
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, RoundingMode.HALF_EVEN), this.currency);
    }

    public Money divide(double divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    @Override
    public int compareTo(Money o) {
        validateSameCurrency(o);
        return this.amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 &&
               currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + " " + amount.toString();
    }
}
