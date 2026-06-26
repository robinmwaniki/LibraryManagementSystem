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
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Check if member can borrow
        if (member.getStatus() != Member.MembershipStatus.ACTIVE) {
            throw new RuntimeException("Member is not active");
        }

        // Check if book is available
        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No copies of this book are available");
        }

        // Check if member has reached maximum borrowing limit
        long activeBorrowings = borrowingRepository.countActiveBorrowingsByMemberId(memberId);
        if (activeBorrowings >= member.getMaxBooksAllowed()) {
            throw new RuntimeException("Member has reached maximum borrowing limit");
        }

        // Create borrowing record
        BorrowingRecord record = new BorrowingRecord();
        record.setBook(book);
        record.setMember(member);
        record.setBorrowDate(LocalDateTime.now());
        int loanPeriodDays = librarySettingsService.getSettings().getLoanPeriodDays();
        record.setDueDate(LocalDateTime.now().plusDays(loanPeriodDays));
        record.setStatus(BorrowingRecord.BorrowStatus.BORROWED);

        // Update book availability
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return borrowingRepository.save(record);
    }

    public BorrowingRecord returnBook(Long borrowingId) {
        BorrowingRecord record = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));

        if (record.getStatus() == BorrowingRecord.BorrowStatus.RETURNED) {
            throw new RuntimeException("Book has already been returned");
        }

        record.setReturnDate(LocalDateTime.now());
        record.setStatus(BorrowingRecord.BorrowStatus.RETURNED);

        // Calculate fine if overdue
        if (record.getReturnDate().isAfter(record.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(record.getDueDate(), record.getReturnDate());
            var settings = librarySettingsService.getSettings();
            double fine = daysLate * settings.getFinePerDay();
            if (settings.getMaxFine() != null && fine > settings.getMaxFine()) {
                fine = settings.getMaxFine();
            }
            record.setFine(fine);
        }

        // Update book availability
        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        return borrowingRepository.save(record);
    }

    public List<BorrowingRecord> getMemberBorrowingHistory(Long memberId) {
        return borrowingRepository.findByMemberId(memberId);
    }

    public List<BorrowingRecord> getActiveBorrowingsByMember(Long memberId) {
        return borrowingRepository.findActiveBorrowingsByMemberId(memberId);
    }

    public List<BorrowingRecord> getOverdueBorrowings() {
        return borrowingRepository.findOverdueBorrowings();
    }

    public BorrowingRecord getBorrowingById(Long id) {
        return borrowingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing record not found"));
    }

    public List<BorrowingRecord> getAllBorrowings() {
        return borrowingRepository.findAll();
    }
}