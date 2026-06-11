package com.lz_Insurance.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;

@Data
@NoArgsConstructor
public class DateRange {

    private LocalDate startDate;
    private LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public static DateRange infiniteFrom(LocalDate startDate) {
        return new DateRange(startDate, LocalDate.of(9999, 12, 31));
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean contains(DateRange other) {
        return contains(other.startDate) && contains(other.endDate);
    }

    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) &&
               !this.startDate.isAfter(other.endDate);
    }

    public long daysBetween() {
        return Period.between(startDate, endDate).getDays();
    }

    public long monthsBetween() {
        return Period.between(startDate, endDate).toTotalMonths();
    }

    public long yearsBetween() {
        return Period.between(startDate, endDate).getYears();
    }

    public DateRange intersect(DateRange other) {
        LocalDate newStart = startDate.isAfter(other.startDate) ? startDate : other.startDate;
        LocalDate newEnd = endDate.isBefore(other.endDate) ? endDate : other.endDate;

        if (newStart.isAfter(newEnd)) {
            return null;
        }

        return new DateRange(newStart, newEnd);
    }
}
