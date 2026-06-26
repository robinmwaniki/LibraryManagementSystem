package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.service.LibrarySettingsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsWebController {

    private static final String SESSION_KEY = "managerAuthenticated";

    private final LibrarySettingsService librarySettingsService;

    @Value("${library.manager.password}")
    private String managerPassword;

    @GetMapping
    public String showSettings(Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/settings/login";
        }
        model.addAttribute("settings", librarySettingsService.getSettings());
        return "settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam Integer loanPeriodDays,
                                 @RequestParam Double finePerDay,
                                 @RequestParam(required = false) Double maxFine,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/settings/login";
        }
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

    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (isAuthenticated(session)) {
            return "redirect:/settings";
        }
        return "settings-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String password,
                        HttpSession session,
                        Model model) {
        if (managerPassword.equals(password)) {
            session.setAttribute(SESSION_KEY, true);
            return "redirect:/settings";
        }
        model.addAttribute("error", "Incorrect password");
        return "settings-login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return "redirect:/";
    }

    private boolean isAuthenticated(HttpSession session) {
        Object flag = session.getAttribute(SESSION_KEY);
        return flag instanceof Boolean && (Boolean) flag;
    }
}