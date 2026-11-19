package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;
    private AuditService auditService;

    @Autowired(required = false)
    public void setAuditService(AuditService auditService) { this.auditService = auditService; }

    public Book addBook(BookDTO bookDTO) {
        String isbn = normalizeIsbn(bookDTO.getIsbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new IllegalArgumentException("A book with this ISBN already exists.");
        }
        Book book = new Book();
        mapDTOToBook(bookDTO, book);
        book.setIsbn(isbn);
        book.setAvailableCopies(bookDTO.getTotalCopies());
        Book saved = bookRepository.save(book);
        audit("CREATE", saved.getId(), null, bookSummary(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    public Book updateBook(Long id, BookDTO bookDTO) {
        Book book = bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        String isbn = normalizeIsbn(bookDTO.getIsbn());
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new IllegalArgumentException("Another book already uses this ISBN.");
        }

        String before = bookSummary(book);
        int currentlyBorrowed = safe(book.getTotalCopies()) - safe(book.getAvailableCopies());
        int requestedTotal = safe(bookDTO.getTotalCopies());
        if (requestedTotal < currentlyBorrowed) {
            throw new IllegalArgumentException("Total copies cannot be less than the number currently borrowed (" + currentlyBorrowed + ").");
        }

        mapDTOToBook(bookDTO, book);
        book.setIsbn(isbn);
        book.setAvailableCopies(requestedTotal - currentlyBorrowed);
        Book saved = bookRepository.save(book);
        audit("UPDATE", saved.getId(), before, bookSummary(saved));
        return saved;
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        if (borrowingRepository.existsByBookId(id)) {
            throw new IllegalStateException("This book has borrowing history and cannot be deleted. Keep the catalogue record for audit purposes.");
        }
        String before = bookSummary(book);
        bookRepository.delete(book);
        audit("DELETE", id, before, null);
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooksByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    @Transactional(readOnly = true)
    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    @Transactional(readOnly = true)
    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return bookRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Book> searchBooks(String search, String category,
            boolean availableOnly, org.springframework.data.domain.Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return bookRepository.searchBooks(normalizedSearch, normalizedCategory, availableOnly, pageable);
    }

    private void mapDTOToBook(BookDTO dto, Book book) {
        book.setIsbn(normalizeIsbn(dto.getIsbn()));
        book.setTitle(dto.getTitle().trim());
        book.setAuthor(dto.getAuthor().trim());
        book.setPublisher(dto.getPublisher());
        book.setPublicationYear(dto.getPublicationYear());
        book.setCategory(dto.getCategory().trim());
        book.setTotalCopies(dto.getTotalCopies());
        book.setShelfLocation(dto.getShelfLocation());
    }

    private void audit(String action, Long id, String before, String after) {
        if (auditService != null) auditService.record(action, "BOOK", id, before, after);
    }

    private String bookSummary(Book book) {
        return "title=" + book.getTitle() + ",category=" + book.getCategory()
                + ",totalCopies=" + safe(book.getTotalCopies()) + ",availableCopies=" + safe(book.getAvailableCopies());
    }

    private String normalizeIsbn(String isbn) {
        return isbn == null ? null : isbn.trim().replace("-", "").replace(" ", "");
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}