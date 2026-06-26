package com.example.librarymanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single-row table holding library-wide settings that the manager
 * can change at runtime instead of having them hardcoded.
 */
@Entity
@Table(name = "library_settings")
@Data
@NoArgsConstructor
public class LibrarySettings {

    @Id
    private Long id = 1L; // always a single row

    @Column(name = "loan_period_days", nullable = false)
    private Integer loanPeriodDays = 14;

    @Column(name = "fine_per_day", nullable = false)
    private Double finePerDay = 1.0;

    @Column(name = "max_fine")
    private Double maxFine; // optional cap, null = no cap
}