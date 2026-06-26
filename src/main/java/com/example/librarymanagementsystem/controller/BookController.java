package com.example.librarymanagementsystem.controller;



import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Book Management", description = "APIs for managing books")
public class BookController {
    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<Book> addBook(@Valid @RequestBody BookDTO bookDTO) {
        return new ResponseEntity<>(bookService.addBook(bookDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all books")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update book")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody BookDTO bookDTO) {
        return ResponseEntity.ok(bookService.updateBook(id, bookDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/title")
    @Operation(summary = "Search books by title")
    public ResponseEntity<List<Book>> searchByTitle(@RequestParam String title) {
        return ResponseEntity.ok(bookService.searchBooksByTitle(title));
    }

    @GetMapping("/search/author")
    @Operation(summary = "Search books by author")
    public ResponseEntity<List<Book>> searchByAuthor(@RequestParam String author) {
        return ResponseEntity.ok(bookService.searchBooksByAuthor(author));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available books")
    public ResponseEntity<List<Book>> getAvailableBooks() {
        return ResponseEntity.ok(bookService.getAvailableBooks());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get books by category")
    public ResponseEntity<List<Book>> getBooksByCategory(@PathVariable String category) {
        return ResponseEntity.ok(bookService.getBooksByCategory(category));
    }
}
