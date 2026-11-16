package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BorrowingService {
    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LibrarySettingsService librarySettingsService;

    public BorrowingRecord borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getStatus() != Member.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Member is not active.");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No copies of this book are available.");
        }

        long activeBorrowings = borrowingRepository.countActiveBorrowingsByMemberId(memberId);
        int maximumAllowed = member.getMaxBooksAllowed() == null ? 5 : member.getMaxBooksAllowed();
        if (activeBorrowings >= maximumAllowed) {
            throw new IllegalStateException("Member has reached the maximum borrowing limit.");
        }

        LocalDateTime borrowedAt = LocalDateTime.now();
        int loanPeriodDays = librarySettingsService.getSettings().getLoanPeriodDays();
        BorrowingRecord record = new BorrowingRecord();
        record.setBook(book);
        record.setMember(member);
        record.setBorrowDate(borrowedAt);
        record.setDueDate(borrowedAt.plusDays(loanPeriodDays));
        record.setStatus(BorrowingRecord.BorrowStatus.BORROWED);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
        return borrowingRepository.save(record);
    }

    public BorrowingRecord returnBook(Long borrowingId) {
        BorrowingRecord record = borrowingRepository.findByIdForUpdate(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));

        if (record.getStatus() == BorrowingRecord.BorrowStatus.RETURNED) {
            throw new IllegalStateException("Book has already been returned.");
        }

        LocalDateTime returnedAt = LocalDateTime.now();
        record.setReturnDate(returnedAt);
        record.setStatus(BorrowingRecord.BorrowStatus.RETURNED);

        if (record.getDueDate() != null && returnedAt.toLocalDate().isAfter(record.getDueDate().toLocalDate())) {
            long daysLate = ChronoUnit.DAYS.between(record.getDueDate().toLocalDate(), returnedAt.toLocalDate());
            var settings = librarySettingsService.getSettings();
            double fine = daysLate * settings.getFinePerDay();
            if (settings.getMaxFine() != null) {
                fine = Math.min(fine, settings.getMaxFine());
            }
            record.setFine(fine);
        } else {
            record.setFine(0.0);
        }

        Book book = bookRepository.findByIdForUpdate(record.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found for this borrowing record"));
        int available = book.getAvailableCopies() == null ? 0 : book.getAvailableCopies();
        int total = book.getTotalCopies() == null ? 0 : book.getTotalCopies();
        if (available >= total) {
            throw new IllegalStateException("Inventory is already at its maximum recorded quantity.");
        }
        book.setAvailableCopies(available + 1);
        bookRepository.save(book);
        return borrowingRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getMemberBorrowingHistory(Long memberId) {
        return borrowingRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getActiveBorrowingsByMember(Long memberId) {
        return borrowingRepository.findActiveBorrowingsByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getOverdueBorrowings() {
        return borrowingRepository.findOverdueBorrowings();
    }

    @Transactional(readOnly = true)
    public BorrowingRecord getBorrowingById(Long id) {
        return borrowingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getAllBorrowings() {
        return borrowingRepository.findAll();
    }
}