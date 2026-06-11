package com.lz_Insurance.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {

    @NotBlank(message = "Street line 1 is required")
    @Size(max = 255, message = "Street line 1 must not exceed 255 characters")
    private String streetLine1;

    @Size(max = 255, message = "Street line 2 must not exceed 255 characters")
    private String streetLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State/Province is required")
    @Size(max = 100, message = "State/Province must not exceed 100 characters")
    private String stateProvince;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-\\s]+$", message = "Postal code must contain only letters, numbers, hyphens, and spaces")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    public static Address of(String streetLine1, String streetLine2, String city,
                             String stateProvince, String postalCode, String country) {
        return new Address(streetLine1, streetLine2, city, stateProvince, postalCode, country);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(streetLine1);
        if (streetLine2 != null && !streetLine2.isBlank()) {
            sb.append(", ").append(streetLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(stateProvince);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }

    public String getShortAddress() {
        return String.format("%s, %s, %s", city, stateProvince, country);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(streetLine1, address.streetLine1) &&
               Objects.equals(streetLine2, address.streetLine2) &&
               Objects.equals(city, address.city) &&
               Objects.equals(stateProvince, address.stateProvince) &&
               Objects.equals(postalCode, address.postalCode) &&
               Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetLine1, streetLine2, city, stateProvince, postalCode, country);
    }
}
