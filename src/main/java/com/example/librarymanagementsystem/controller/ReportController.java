package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import com.example.librarymanagementsystem.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;
    private final BorrowingService borrowingService;

    @GetMapping(value = "/overdue.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportOverdueLoans() {
        StringBuilder csv = new StringBuilder("Borrowing ID,Book,Member,Email,Due Date,Fine\r\n");
        for (BorrowingRecord loan : borrowingService.getOverdueBorrowings()) {
            csv.append(loan.getId()).append(',')
                    .append(csvValue(loan.getBook().getTitle())).append(',')
                    .append(csvValue(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())).append(',')
                    .append(csvValue(loan.getMember().getEmail())).append(',')
                    .append(csvValue(format(loan.getDueDate()))).append(',')
                    .append(loan.getFine() == null ? "0.00" : BigDecimal.valueOf(loan.getFine()).toPlainString())
                    .append("\r\n");
        }
        return csvResponse(csv, "overdue-loans.csv");
    }

    @GetMapping
    public String reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) BorrowingRecord.BorrowStatus status,
            Model model) {
        boolean invalidRange = startDate != null && endDate != null && startDate.isAfter(endDate);
        LocalDateTime startInclusive = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        var records = invalidRange ? java.util.List.<BorrowingRecord>of()
                : borrowingRepository.findReportBorrowings(startInclusive, endExclusive,
                    normalizeCategory(category), status);

        var popularBooks = records.stream()
                .collect(Collectors.groupingBy(record -> record.getBook().getTitle(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5).toList();
        var activeMembers = records.stream()
                .collect(Collectors.groupingBy(record -> record.getMember().getFirstName() + " "
                        + record.getMember().getLastName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5).toList();

        model.addAttribute("totalTitles", bookRepository.count());
        model.addAttribute("totalCopies", bookRepository.sumTotalCopies());
        model.addAttribute("availableCopies", bookRepository.sumAvailableCopies());
        model.addAttribute("activeMembers", memberRepository.countByStatus(
                com.example.librarymanagementsystem.model.Member.MembershipStatus.ACTIVE));
        model.addAttribute("activeLoans", borrowingRepository.countByStatus(BorrowingRecord.BorrowStatus.BORROWED));
        model.addAttribute("overdueCount", records.stream().filter(this::isOverdue).count());
        model.addAttribute("recordedFines", records.stream().map(BorrowingRecord::getFine)
                .filter(Objects::nonNull).map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("records", records);
        model.addAttribute("recordCount", records.size());
        model.addAttribute("popularBooks", popularBooks);
        model.addAttribute("activeMembersByLoans", activeMembers);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("selectedStatus", status == null ? "" : status.name());
        model.addAttribute("invalidRange", invalidRange);
        model.addAttribute("categories", bookRepository.findDistinctCategories());
        return "reports";
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportFilteredReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) BorrowingRecord.BorrowStatus status) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        var records = borrowingRepository.findReportBorrowings(
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                normalizeCategory(category), status);
        StringBuilder csv = new StringBuilder(
                "Borrowing ID,Book,Category,Member,Email,Borrowed At,Due Date,Returned At,Status,Fine\r\n");
        for (BorrowingRecord loan : records) {
            csv.append(loan.getId()).append(',')
                    .append(csvValue(loan.getBook().getTitle())).append(',')
                    .append(csvValue(loan.getBook().getCategory())).append(',')
                    .append(csvValue(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())).append(',')
                    .append(csvValue(loan.getMember().getEmail())).append(',')
                    .append(csvValue(format(loan.getBorrowDate()))).append(',')
                    .append(csvValue(format(loan.getDueDate()))).append(',')
                    .append(csvValue(format(loan.getReturnDate()))).append(',')
                    .append(loan.getStatus().name()).append(',')
                    .append(loan.getFine() == null ? "0.00" : BigDecimal.valueOf(loan.getFine()).toPlainString())
                    .append("\r\n");
        }
        return csvResponse(csv, "library-report.csv");
    }

    private ResponseEntity<byte[]> csvResponse(StringBuilder csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }

    private boolean isOverdue(BorrowingRecord record) {
        return record.getStatus() == BorrowingRecord.BorrowStatus.BORROWED
                && record.getDueDate() != null && record.getDueDate().isBefore(LocalDateTime.now());
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(CSV_DATE);
    }

    /**
     * Quotes CSV fields, doubles embedded quotes and prefixes spreadsheet formula
     * initiators (including control-character variants) to prevent formula injection.
     */
    private String csvValue(String value) {
        if (value == null) return "\"\"";
        String safe = value;
        if (!safe.isEmpty()) {
            char first = safe.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@'
                    || first == '\t' || first == '\r' || first == '\n') {
                safe = "'" + safe;
            }
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}