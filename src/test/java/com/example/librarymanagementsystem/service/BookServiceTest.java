package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private AuditService auditService;
    @InjectMocks private BookService bookService;

    @Test
    void updateBookPreservesCopiesCurrentlyBorrowed() {
        Book existing = new Book();
        existing.setId(7L);
        existing.setIsbn("9780000000001");
        existing.setTitle("Old title");
        existing.setAuthor("Author");
        existing.setTotalCopies(10);
        existing.setAvailableCopies(7);

        when(bookRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnAndIdNot("9780000000002", 7L)).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO dto = new BookDTO();
        dto.setIsbn("978-0-000-00000-2");
        dto.setTitle("Updated title");
        dto.setAuthor("Updated author");
        dto.setCategory("Technology");
        dto.setTotalCopies(12);

        Book updated = bookService.updateBook(7L, dto);

        assertEquals(3, 12 - updated.getAvailableCopies());
        assertEquals(9, updated.getAvailableCopies());
        assertEquals(12, updated.getTotalCopies());
        assertEquals("9780000000002", updated.getIsbn());
        verify(auditService).record(eq("UPDATE"), eq("BOOK"), eq(7L), anyString(), anyString());
    }

    @Test
    void updateBookRejectsTotalBelowBorrowedQuantity() {
        Book existing = new Book();
        existing.setId(7L);
        existing.setIsbn("9780000000001");
        existing.setTotalCopies(10);
        existing.setAvailableCopies(7);
        when(bookRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnAndIdNot("9780000000002", 7L)).thenReturn(false);

        BookDTO dto = new BookDTO();
        dto.setIsbn("9780000000002");
        dto.setTitle("Updated title");
        dto.setAuthor("Updated author");
        dto.setCategory("Technology");
        dto.setTotalCopies(2);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(7L, dto));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void searchBooksUsesDatabasePaginationAndReportsTotalResults() {
        Book book = new Book();
        book.setId(21L);
        book.setTitle("Spring in Action");
        Pageable pageable = PageRequest.of(1, 5);
        Page<Book> repositoryPage = new PageImpl<>(List.of(book), pageable, 11);
        when(bookRepository.searchBooks("spring", "Technology", true, pageable)).thenReturn(repositoryPage);

        Page<Book> result = bookService.searchBooks(" spring ", " Technology ", true, pageable);

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(11, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(List.of(book), result.getContent());
        verify(bookRepository).searchBooks("spring", "Technology", true, pageable);
    }

    @Test
    void searchBooksReturnsAnEmptyPageWhenRequestedPageIsOutOfRange() {
        Pageable pageable = PageRequest.of(4, 5);
        Page<Book> emptyPage = new PageImpl<>(List.of(), pageable, 11);
        when(bookRepository.searchBooks(isNull(), isNull(), eq(false), eq(pageable))).thenReturn(emptyPage);

        Page<Book> result = bookService.searchBooks("", "", false, pageable);

        assertTrue(result.isEmpty());
        assertEquals(11, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }
}