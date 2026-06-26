package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.LibrarySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibrarySettingsRepository extends JpaRepository<LibrarySettings, Long> {
}