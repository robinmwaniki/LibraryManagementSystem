package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsWebController {
    private final LibrarySettingsService librarySettingsService;

    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("settings", librarySettingsService.getSettings());
        return "settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam Integer loanPeriodDays,
                                 @RequestParam Double finePerDay,
                                 @RequestParam(required = false) Double maxFine,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            librarySettingsService.updateSettings(loanPeriodDays, finePerDay, maxFine);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("settings", librarySettingsService.getSettings());
            return "settings";
        }
        redirectAttributes.addFlashAttribute("success", "Settings updated successfully!");
        return "redirect:/settings";
    }
}