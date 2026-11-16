package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final BookRepository bookRepository;

    public Book addBook(BookDTO bookDTO) {
        String isbn = normalizeIsbn(bookDTO.getIsbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new IllegalArgumentException("A book with this ISBN already exists.");
        }
        Book book = new Book();
        mapDTOToBook(bookDTO, book);
        book.setIsbn(isbn);
        book.setAvailableCopies(bookDTO.getTotalCopies());
        return bookRepository.save(book);
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

        int currentlyBorrowed = safe(book.getTotalCopies()) - safe(book.getAvailableCopies());
        int requestedTotal = safe(bookDTO.getTotalCopies());
        if (requestedTotal < currentlyBorrowed) {
            throw new IllegalArgumentException("Total copies cannot be less than the number currently borrowed (" + currentlyBorrowed + ").");
        }

        mapDTOToBook(bookDTO, book);
        book.setIsbn(isbn);
        book.setAvailableCopies(requestedTotal - currentlyBorrowed);
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        if (safe(book.getAvailableCopies()) != safe(book.getTotalCopies())) {
            throw new IllegalStateException("This book has active borrowings and cannot be deleted.");
        }
        bookRepository.delete(book);
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

    private String normalizeIsbn(String isbn) {
        return isbn == null ? null : isbn.trim().replace("-", "").replace(" ", "");
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}