package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.LibrarySettings;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for inventory changes and business rules in borrowing workflows.
 */
@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LibrarySettingsService librarySettingsService;
    @InjectMocks private BorrowingService borrowingService;

    @Test
    void borrowBookDecrementsAvailableStock() {
        Book book = new Book();
        book.setId(1L);
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        Member member = new Member();
        member.setId(2L);
        member.setStatus(Member.MembershipStatus.ACTIVE);
        member.setMaxBooksAllowed(5);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(member));
        when(borrowingRepository.countActiveBorrowingsByMemberId(2L)).thenReturn(0L);
        when(librarySettingsService.getSettings()).thenReturn(new LibrarySettings());
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));

        BorrowingRecord record = borrowingService.borrowBook(1L, 2L);

        assertEquals(0, book.getAvailableCopies());
        assertEquals(BorrowingRecord.BorrowStatus.BORROWED, record.getStatus());
        assertNotNull(record.getDueDate());
    }

    @Test
    void borrowBookRejectsNonActiveMembersWithoutCreatingALoan() {
        Book book = new Book();
        book.setId(1L);
        book.setAvailableCopies(1);
        Member member = new Member();
        member.setId(2L);
        member.setStatus(Member.MembershipStatus.SUSPENDED);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(member));

        assertThrows(IllegalStateException.class, () -> borrowingService.borrowBook(1L, 2L));

        verify(borrowingRepository, never()).save(any(BorrowingRecord.class));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void borrowBookRejectsUnavailableInventory() {
        Book book = new Book();
        book.setId(1L);
        book.setAvailableCopies(0);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        assertThrows(IllegalStateException.class, () -> borrowingService.borrowBook(1L, 2L));

        verify(memberRepository, never()).findByIdForUpdate(anyLong());
        verify(borrowingRepository, never()).save(any(BorrowingRecord.class));
    }

    @Test
    void returnBookRestoresStockAndRejectsSecondReturn() {
        Book book = new Book();
        book.setId(1L);
        book.setTotalCopies(1);
        book.setAvailableCopies(0);
        BorrowingRecord record = new BorrowingRecord();
        record.setId(3L);
        record.setBook(book);
        record.setStatus(BorrowingRecord.BorrowStatus.BORROWED);
        record.setDueDate(LocalDateTime.now().plusDays(2));

        when(borrowingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(record));
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));

        BorrowingRecord returned = borrowingService.returnBook(3L);
        assertEquals(BorrowingRecord.BorrowStatus.RETURNED, returned.getStatus());
        assertEquals(1, book.getAvailableCopies());

        assertThrows(IllegalStateException.class, () -> borrowingService.returnBook(3L));
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void returnBookCalculatesOverdueFineAndAppliesConfiguredMaximum() {
        Book book = new Book();
        book.setId(1L);
        book.setTotalCopies(1);
        book.setAvailableCopies(0);

        BorrowingRecord record = new BorrowingRecord();
        record.setId(4L);
        record.setBook(book);
        record.setStatus(BorrowingRecord.BorrowStatus.BORROWED);
        record.setDueDate(LocalDateTime.now().minusDays(3));

        LibrarySettings settings = new LibrarySettings();
        settings.setFinePerDay(5.0);
        settings.setMaxFine(10.0);

        when(borrowingRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(record));
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(librarySettingsService.getSettings()).thenReturn(settings);
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));

        BorrowingRecord returned = borrowingService.returnBook(4L);

        // Three overdue days would total 15.00, so the configured cap must apply.
        assertEquals(10.0, returned.getFine());
        assertEquals(BorrowingRecord.BorrowStatus.RETURNED, returned.getStatus());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void renewBookAllowsOnlyOneRenewalForAnActiveLoan() {
        BorrowingRecord record = new BorrowingRecord();
        record.setId(9L);
        record.setStatus(BorrowingRecord.BorrowStatus.BORROWED);
        LocalDateTime originalDueDate = LocalDateTime.now().plusDays(2);
        record.setDueDate(originalDueDate);
        record.setRenewalCount(0);

        LibrarySettings settings = new LibrarySettings();
        settings.setLoanPeriodDays(14);
        when(borrowingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(record));
        when(librarySettingsService.getSettings()).thenReturn(settings);
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));

        BorrowingRecord renewed = borrowingService.renewBook(9L);
        assertEquals(1, renewed.getRenewalCount());
        assertEquals(originalDueDate.plusDays(14), renewed.getDueDate());

        assertThrows(IllegalStateException.class, () -> borrowingService.renewBook(9L));
        verify(borrowingRepository, times(1)).save(record);
    }
}
