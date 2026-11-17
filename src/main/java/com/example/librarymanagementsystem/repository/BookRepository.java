package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByCategory(String category);

    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0")
    List<Book> findAvailableBooks();

    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL AND TRIM(b.category) <> '' ORDER BY b.category")
    List<String> findDistinctCategories();

    /**
     * Applies one partial, case-insensitive search term across the catalogue's
     * identifying fields before filtering and paging in the database.
     */
    @Query("""
        SELECT b FROM Book b
        WHERE (:term IS NULL OR :term = ''
            OR LOWER(b.title) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(b.author) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(COALESCE(b.isbn, '')) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(COALESCE(b.publisher, '')) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(COALESCE(b.category, '')) LIKE LOWER(CONCAT('%', :term, '%')))
          AND (:category IS NULL OR :category = '' OR LOWER(b.category) = LOWER(:category))
          AND (:availableOnly = false OR b.availableCopies > 0)
        """)
    Page<Book> searchBooks(@Param("term") String term,
                           @Param("category") String category,
                           @Param("availableOnly") boolean availableOnly,
                           Pageable pageable);

    List<Book> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.totalCopies), 0) FROM Book b")
    Long sumTotalCopies();

    @Query("SELECT COALESCE(SUM(b.availableCopies), 0) FROM Book b")
    Long sumAvailableCopies();
}
