package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CatalogueCacheIntegrationTest {
    @Autowired private BookService bookService;
    @Autowired private CacheManager cacheManager;
    @MockBean private BookRepository bookRepository;
    @MockBean private BorrowingRepository borrowingRepository;
    @MockBean private AuditService auditService;

    @BeforeEach
    void clearCachesAndMocks() {
        cacheManager.getCache("catalogueSearch").clear();
        cacheManager.getCache("catalogueCategories").clear();
        reset(bookRepository, borrowingRepository, auditService);
    }

    @Test
    void cachesSearchAndCategoriesUntilCatalogueMutationEvictsBoth() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(), pageable, 0);
        when(bookRepository.searchBooks(eq("java"), isNull(), eq(false), eq(pageable))).thenReturn(page);
        when(bookRepository.findDistinctCategories()).thenReturn(List.of("Fiction", "Technology"));

        assertSame(page, bookService.searchBooks("java", null, false, pageable));
        assertSame(page, bookService.searchBooks("java", null, false, pageable));
        assertEquals(List.of("Fiction", "Technology"), bookService.getCategories());
        assertEquals(List.of("Fiction", "Technology"), bookService.getCategories());

        verify(bookRepository, times(1)).searchBooks(eq("java"), isNull(), eq(false), eq(pageable));
        verify(bookRepository, times(1)).findDistinctCategories();

        Book existing = new Book();
        existing.setTitle("Cache invalidation fixture");
        existing.setAuthor("Test");
        existing.setCategory("Fiction");
        existing.setTotalCopies(1);
        existing.setAvailableCopies(1);
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(borrowingRepository.existsByBookId(5L)).thenReturn(false);

        bookService.deleteBook(5L);

        bookService.searchBooks("java", null, false, pageable);
        bookService.getCategories();
        verify(bookRepository, times(2)).searchBooks(eq("java"), isNull(), eq(false), eq(pageable));
        verify(bookRepository, times(2)).findDistinctCategories();
        verify(bookRepository).delete(existing);
    }

    @Test
    void cacheManagerExposesOnlyTheBoundedCatalogueCaches() {
        assertNotNull(cacheManager.getCache("catalogueSearch"));
        assertNotNull(cacheManager.getCache("catalogueCategories"));
        assertNull(cacheManager.getCache("memberRecords"));
    }
}
