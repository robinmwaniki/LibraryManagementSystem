package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.model.BookReservation.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, List<ReservationStatus> statuses);

    List<BookReservation> findByBookIdAndStatusOrderByCreatedAtAscIdAsc(Long bookId, ReservationStatus status);

    List<BookReservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<BookReservation> findAllByOrderByCreatedAtAscIdAsc();

    List<BookReservation> findByBookIdOrderByCreatedAtAscIdAsc(Long bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BookReservation r WHERE r.id = :id")
    Optional<BookReservation> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM BookReservation r WHERE r.status = :status AND r.expiresAt <= :now")
    List<BookReservation> findExpiredReadyReservations(@Param("status") ReservationStatus status,
                                                        @Param("now") LocalDateTime now);
}
