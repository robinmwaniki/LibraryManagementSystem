package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Library Dashboard");
        model.addAttribute("stats", Map.of(
                "bookTitles", bookRepository.count(),
                "totalCopies", bookRepository.sumTotalCopies(),
                "availableCopies", bookRepository.sumAvailableCopies(),
                "activeMembers", memberRepository.countByStatus(Member.MembershipStatus.ACTIVE),
                "activeBorrowings", borrowingRepository.countByStatus(BorrowingRecord.BorrowStatus.BORROWED),
                "overdueBorrowings", borrowingRepository.countOverdueBorrowings()
        ));
        List<Book> recentBooks = bookRepository.findTop5ByOrderByCreatedAtDesc();
        model.addAttribute("recentBooks", recentBooks);
        return "index";
    }
}