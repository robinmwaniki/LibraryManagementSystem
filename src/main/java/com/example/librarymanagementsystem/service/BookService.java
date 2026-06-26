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
        if (bookRepository.existsByIsbn(bookDTO.getIsbn())) {
            throw new RuntimeException("Book with ISBN " + bookDTO.getIsbn() + " already exists");
        }

        Book book = new Book();
        mapDTOToBook(bookDTO, book);
        return bookRepository.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    public Book updateBook(Long id, BookDTO bookDTO) {
        Book book = getBookById(id);
        mapDTOToBook(bookDTO, book);
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> searchBooksByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findByCategory(category);
    }

    private void mapDTOToBook(BookDTO dto, Book book) {
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPublicationYear(dto.getPublicationYear());
        book.setCategory(dto.getCategory());
        book.setTotalCopies(dto.getTotalCopies());
        book.setShelfLocation(dto.getShelfLocation());

        if (dto.getAvailableCopies() != null) {
            book.setAvailableCopies(dto.getAvailableCopies());
        } else {
            book.setAvailableCopies(dto.getTotalCopies());
        }
    }
}
