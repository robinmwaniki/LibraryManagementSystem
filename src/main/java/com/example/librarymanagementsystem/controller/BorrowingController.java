package com.example.librarymanagementsystem.controller;

// src/main/java/com/library/controller/BorrowingController.java


import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.service.BorrowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
@Tag(name = "Borrowing Management", description = "APIs for managing book borrowings")
public class BorrowingController {
    private final BorrowingService borrowingService;

    @PostMapping("/borrow")
    @Operation(summary = "Borrow a book")
    public ResponseEntity<BorrowingRecord> borrowBook(@RequestParam Long bookId, @RequestParam Long memberId) {
        return new ResponseEntity<>(borrowingService.borrowBook(bookId, memberId), HttpStatus.CREATED);
    }

    @PostMapping("/{borrowingId}/renew")
    @Operation(summary = "Renew an active book loan once")
    public ResponseEntity<BorrowingRecord> renewBook(@PathVariable Long borrowingId) {
        return ResponseEntity.ok(borrowingService.renewBook(borrowingId));
    }

    @PostMapping("/return/{borrowingId}")
    @Operation(summary = "Return a book")
    public ResponseEntity<BorrowingRecord> returnBook(@PathVariable Long borrowingId) {
        return ResponseEntity.ok(borrowingService.returnBook(borrowingId));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get member's borrowing history")
    public ResponseEntity<List<BorrowingRecord>> getMemberBorrowingHistory(@PathVariable Long memberId) {
        return ResponseEntity.ok(borrowingService.getMemberBorrowingHistory(memberId));
    }

    @GetMapping("/member/{memberId}/active")
    @Operation(summary = "Get member's active borrowings")
    public ResponseEntity<List<BorrowingRecord>> getActiveBorrowings(@PathVariable Long memberId) {
        return ResponseEntity.ok(borrowingService.getActiveBorrowingsByMember(memberId));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get all overdue borrowings")
    public ResponseEntity<List<BorrowingRecord>> getOverdueBorrowings() {
        return ResponseEntity.ok(borrowingService.getOverdueBorrowings());
    }

    @GetMapping
    @Operation(summary = "Get all borrowing records")
    public ResponseEntity<List<BorrowingRecord>> getAllBorrowings() {
        return ResponseEntity.ok(borrowingService.getAllBorrowings());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get borrowing record by ID")
    public ResponseEntity<BorrowingRecord> getBorrowingById(@PathVariable Long id) {
        return ResponseEntity.ok(borrowingService.getBorrowingById(id));
    }
}
