package com.example.jpajavaexample.domain.performance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Performance(String title, String category, LocalDate startDate, LocalDate endDate) {
        this.title = Objects.requireNonNull(title, "title");
        this.category = Objects.requireNonNull(category, "category");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
        validatePeriod(this.startDate, this.endDate);
    }

    public static Performance create(String title, String category, LocalDate startDate, LocalDate endDate) {
        return new Performance(title, category, startDate, endDate);
    }

    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        LocalDate newStart = Objects.requireNonNull(startDate, "startDate");
        LocalDate newEnd = Objects.requireNonNull(endDate, "endDate");
        validatePeriod(newStart, newEnd);
        this.startDate = newStart;
        this.endDate = newEnd;
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Performance endDate must be the same as or after startDate");
        }
    }
}
