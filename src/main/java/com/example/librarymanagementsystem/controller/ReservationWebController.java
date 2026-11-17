package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationWebController {
    private final ReservationService reservationService;

    @GetMapping
    public String listReservations(Model model) {
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "reservations/list";
    }

    @PostMapping("/{id}/collect")
    public String collectReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.collectReservation(id);
            redirectAttributes.addFlashAttribute("success", "Reservation collected and loan created.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    @PostMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @RequestParam Long memberId,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservation(id, memberId);
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled and queue updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reservations";
    }
}
