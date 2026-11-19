package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import com.example.librarymanagementsystem.service.BorrowingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportControllerTest {
    @Test
    void reportAppliesFiltersAndUsesExclusiveNextDayBoundary() {
        BookRepository books = mock(BookRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        BorrowingRepository borrowings = mock(BorrowingRepository.class);
        BorrowingService borrowingService = mock(BorrowingService.class);
        when(borrowings.findReportBorrowings(any(), any(), any(), any())).thenReturn(List.of());
        when(books.findAll()).thenReturn(List.of());
        ReportController controller = new ReportController(books, members, borrowings, borrowingService);
        controller.reports(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                " Technology ", BorrowingRecord.BorrowStatus.RETURNED, new ExtendedModelMap());
        verify(borrowings).findReportBorrowings(
                eq(LocalDate.of(2026, 11, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 12, 1).atStartOfDay()),
                eq("Technology"), eq(BorrowingRecord.BorrowStatus.RETURNED));
    }

    @Test
    void invalidDateRangeDoesNotQueryAndDisplaysValidationState() {
        BookRepository books = mock(BookRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        BorrowingRepository borrowings = mock(BorrowingRepository.class);
        BorrowingService borrowingService = mock(BorrowingService.class);
        when(books.findAll()).thenReturn(List.of());
        ReportController controller = new ReportController(books, members, borrowings, borrowingService);
        ExtendedModelMap model = new ExtendedModelMap();
        controller.reports(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), "", null, model);
        verify(borrowings, never()).findReportBorrowings(any(), any(), any(), any());
        assertEquals(true, model.get("invalidRange"));
        assertEquals(0, model.get("recordCount"));
    }

    @Test
    void filteredCsvQuotesFieldsAndNeutralizesSpreadsheetFormulas() {
        Book book = mock(Book.class);
        Member member = mock(Member.class);
        BorrowingRecord record = mock(BorrowingRecord.class);
        when(book.getTitle()).thenReturn("=HYPERLINK(\"https://bad.example\")");
        when(book.getCategory()).thenReturn("Technology");
        when(member.getFirstName()).thenReturn("Robin");
        when(member.getLastName()).thenReturn("Mwaniki");
        when(member.getEmail()).thenReturn("robin@example.test");
        when(record.getId()).thenReturn(42L);
        when(record.getBook()).thenReturn(book);
        when(record.getMember()).thenReturn(member);
        when(record.getBorrowDate()).thenReturn(LocalDateTime.of(2026, 11, 1, 9, 30));
        when(record.getDueDate()).thenReturn(LocalDateTime.of(2026, 11, 8, 9, 30));
        when(record.getReturnDate()).thenReturn(null);
        when(record.getStatus()).thenReturn(BorrowingRecord.BorrowStatus.BORROWED);
        when(record.getFine()).thenReturn(15.0);
        BorrowingRepository borrowings = mock(BorrowingRepository.class);
        when(borrowings.findReportBorrowings(any(), any(), any(), any())).thenReturn(List.of(record));
        ReportController controller = new ReportController(mock(BookRepository.class),
                mock(MemberRepository.class), borrowings, mock(BorrowingService.class));
        var response = controller.exportFilteredReport(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), "Technology", BorrowingRecord.BorrowStatus.BORROWED);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String csv = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://bad.example\"\")\""));
        assertTrue(csv.contains("Robin Mwaniki"));
        assertTrue(csv.contains("2026-11-01 09:30"));
        assertTrue(csv.contains("15.0"));
        verify(borrowings).findReportBorrowings(eq(LocalDate.of(2026, 11, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 12, 1).atStartOfDay()), eq("Technology"),
                eq(BorrowingRecord.BorrowStatus.BORROWED));
    }

    @Test
    void invalidCsvDateRangeReturnsBadRequest() {
        BorrowingRepository borrowings = mock(BorrowingRepository.class);
        ReportController controller = new ReportController(mock(BookRepository.class),
                mock(MemberRepository.class), borrowings, mock(BorrowingService.class));
        var response = controller.exportFilteredReport(LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 11, 1), "", null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(borrowings);
    }
}