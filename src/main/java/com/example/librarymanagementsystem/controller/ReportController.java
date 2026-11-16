package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import com.example.librarymanagementsystem.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;
    private final BorrowingService borrowingService;

    @GetMapping
    public String reports(Model model) {
        model.addAttribute("totalTitles", bookRepository.count());
        model.addAttribute("totalCopies", bookRepository.sumTotalCopies());
        model.addAttribute("availableCopies", bookRepository.sumAvailableCopies());
        model.addAttribute("activeMembers", memberRepository.countByStatus(
                com.example.librarymanagementsystem.model.Member.MembershipStatus.ACTIVE));
        model.addAttribute("activeLoans", borrowingRepository.countByStatus(BorrowingRecord.BorrowStatus.BORROWED));
        model.addAttribute("overdueLoans", borrowingService.getOverdueBorrowings());
        model.addAttribute("overdueCount", borrowingRepository.countOverdueBorrowings());
        model.addAttribute("recordedFines", borrowingRepository.sumRecordedFines());
        return "reports";
    }
}