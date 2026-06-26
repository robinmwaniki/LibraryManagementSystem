package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.service.BookService;
import com.example.librarymanagementsystem.service.BorrowingService;
import com.example.librarymanagementsystem.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Renders the HTML pages for Borrowings (list / borrow / return).
 * The JSON REST API for borrowings lives separately in BorrowingController under /api/borrowings.
 */
@Controller
@RequestMapping("/borrowings")
@RequiredArgsConstructor
public class BorrowingWebController {

    private final BorrowingService borrowingService;
    private final BookService bookService;
    private final MemberService memberService;

    @GetMapping
    public String listBorrowings(Model model) {
        model.addAttribute("borrowings", borrowingService.getAllBorrowings());
        model.addAttribute("overdueBorrowings", borrowingService.getOverdueBorrowings());
        return "borrowings/list";
    }

    @GetMapping("/borrow")
    public String showBorrowForm(Model model) {
        model.addAttribute("availableBooks", bookService.getAvailableBooks());
        model.addAttribute("activeMembers", memberService.getAllMembers().stream()
                .filter(m -> m.getStatus() == Member.MembershipStatus.ACTIVE)
                .toList());
        return "borrowings/borrow";
    }

    @PostMapping("/borrow")
    public String borrowBook(@RequestParam Long bookId,
                              @RequestParam Long memberId,
                              Model model) {
        try {
            borrowingService.borrowBook(bookId, memberId);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("availableBooks", bookService.getAvailableBooks());
            model.addAttribute("activeMembers", memberService.getAllMembers().stream()
                    .filter(m -> m.getStatus() == Member.MembershipStatus.ACTIVE)
                    .toList());
            return "borrowings/borrow";
        }
        return "redirect:/borrowings";
    }

    @GetMapping("/return")
    public String showReturnForm(@RequestParam(required = false) Long memberId, Model model) {
        model.addAttribute("members", memberService.getAllMembers());
        model.addAttribute("selectedMemberId", memberId);

        if (memberId != null) {
            Member member = memberService.getMemberById(memberId);
            List<BorrowingRecord> activeBorrowings = borrowingService.getActiveBorrowingsByMember(memberId);
            long overdueCount = activeBorrowings.stream()
                    .filter(b -> b.getDueDate() != null && b.getDueDate().isBefore(LocalDateTime.now()))
                    .count();

            model.addAttribute("memberName", member.getFirstName() + " " + member.getLastName());
            model.addAttribute("activeBorrowings", activeBorrowings);
            model.addAttribute("overdueCount", overdueCount);
        } else {
            model.addAttribute("activeBorrowings", Collections.emptyList());
        }
        return "borrowings/return";
    }

    @PostMapping("/return/{id}")
    public String returnBook(@PathVariable Long id, @RequestParam(required = false) Long memberId) {
        borrowingService.returnBook(id);
        String redirect = "redirect:/borrowings/return";
        if (memberId != null) {
            redirect += "?memberId=" + memberId;
        }
        return redirect;
    }

    @GetMapping("/member/{id}")
    public String borrowingHistoryForMember(@PathVariable Long id, Model model) {
        model.addAttribute("borrowings", borrowingService.getMemberBorrowingHistory(id));
        model.addAttribute("overdueBorrowings", borrowingService.getOverdueBorrowings());
        return "borrowings/list";
    }
}
