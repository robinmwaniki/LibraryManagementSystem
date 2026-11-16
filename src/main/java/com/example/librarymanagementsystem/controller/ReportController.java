package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import com.example.librarymanagementsystem.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
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

    @GetMapping(value = "/overdue.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportOverdueLoans() {
        StringBuilder csv = new StringBuilder("Borrowing ID,Book,Member,Email,Due Date,Fine\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (BorrowingRecord loan : borrowingService.getOverdueBorrowings()) {
            csv.append(loan.getId()).append(',')
                    .append(csvValue(loan.getBook().getTitle())).append(',')
                    .append(csvValue(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())).append(',')
                    .append(csvValue(loan.getMember().getEmail())).append(',')
                    .append(loan.getDueDate() == null ? "" : loan.getDueDate().format(formatter)).append(',')
                    .append(loan.getFine() == null ? "0.00" : loan.getFine()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overdue-loans.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvValue(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

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