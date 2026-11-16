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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository bookRepository;
    @Mock private BorrowingRepository borrowingRepository;
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
}