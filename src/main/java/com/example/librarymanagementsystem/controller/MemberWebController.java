package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.dto.MemberDTO;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.service.BorrowingService;
import com.example.librarymanagementsystem.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Renders the HTML pages for Members (list / add / edit / delete).
 * The JSON REST API for members lives separately in MemberController under /api/members.
 */
@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberWebController {

    private final MemberService memberService;
    private final BorrowingService borrowingService;

    @GetMapping
    public String listMembers(Model model) {
        model.addAttribute("members", memberService.getAllMembers());
        return "members/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("member", new MemberDTO());
        return "members/add";
    }

    @PostMapping("/add")
    public String registerMember(@Valid @ModelAttribute("member") MemberDTO member,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "members/add";
        }
        try {
            memberService.registerMember(member);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "members/add";
        }
        redirectAttributes.addFlashAttribute("success", "Member registered successfully!");
        return "redirect:/members";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Member member = memberService.getMemberById(id);
        model.addAttribute("member", member);
        model.addAttribute("activeBorrowings",
                borrowingService.getActiveBorrowingsByMember(id).size());
        return "members/edit";
    }

    @PutMapping("/update/{id}")
    public String updateMember(@PathVariable Long id,
                               @Valid @ModelAttribute("member") MemberDTO member,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("member", memberService.getMemberById(id));
            return "members/edit";
        }
        try {
            memberService.updateMember(id, member);
            if (member.getStatus() != null) {
                memberService.updateMemberStatus(id, member.getStatus());
            }
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("member", memberService.getMemberById(id));
            return "members/edit";
        }
        redirectAttributes.addFlashAttribute("success", "Member updated successfully!");
        return "redirect:/members";
    }

    @PostMapping("/delete/{id}")
    public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("success", "Member deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/members";
    }
}