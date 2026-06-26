package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.LibrarySettings;
import com.example.librarymanagementsystem.repository.LibrarySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LibrarySettingsService {

    private final LibrarySettingsRepository settingsRepository;

    /**
     * Returns the single settings row, creating it with sensible
     * defaults the first time the app runs.
     */
    public LibrarySettings getSettings() {
        return settingsRepository.findById(1L)
                .orElseGet(() -> settingsRepository.save(new LibrarySettings()));
    }

    public LibrarySettings updateSettings(Integer loanPeriodDays, Double finePerDay, Double maxFine) {
        LibrarySettings settings = getSettings();

        if (loanPeriodDays == null || loanPeriodDays < 1) {
            throw new RuntimeException("Loan period must be at least 1 day");
        }
        if (finePerDay == null || finePerDay < 0) {
            throw new RuntimeException("Fine per day cannot be negative");
        }
        if (maxFine != null && maxFine < 0) {
            throw new RuntimeException("Maximum fine cannot be negative");
        }

        settings.setLoanPeriodDays(loanPeriodDays);
        settings.setFinePerDay(finePerDay);
        settings.setMaxFine(maxFine);

        return settingsRepository.save(settings);
    }
}