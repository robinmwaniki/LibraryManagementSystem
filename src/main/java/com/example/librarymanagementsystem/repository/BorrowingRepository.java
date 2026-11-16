package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface BorrowingRepository extends JpaRepository<BorrowingRecord, Long> {
    List<BorrowingRecord> findByMemberId(Long memberId);
    List<BorrowingRecord> findByBookId(Long bookId);

    @Query("SELECT br FROM BorrowingRecord br WHERE br.member.id = :memberId AND br.status = 'BORROWED'")
    List<BorrowingRecord> findActiveBorrowingsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(br) FROM BorrowingRecord br WHERE br.member.id = :memberId AND br.status = 'BORROWED'")
    Long countActiveBorrowingsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT br FROM BorrowingRecord br WHERE br.status = 'BORROWED' AND br.dueDate < CURRENT_TIMESTAMP")
    List<BorrowingRecord> findOverdueBorrowings();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT br FROM BorrowingRecord br WHERE br.id = :id")
    Optional<BorrowingRecord> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(BorrowingRecord.BorrowStatus status);

    boolean existsByMemberId(Long memberId);

    @Query("SELECT COUNT(br) FROM BorrowingRecord br WHERE br.status = 'BORROWED' AND br.dueDate < CURRENT_TIMESTAMP")
    long countOverdueBorrowings();

    @Query("SELECT COALESCE(SUM(br.fine), 0) FROM BorrowingRecord br")
    Double sumRecordedFines();

    @Query("SELECT br FROM BorrowingRecord br WHERE br.status = 'BORROWED' AND br.dueDate < CURRENT_TIMESTAMP AND (br.lastReminderSentAt IS NULL OR br.lastReminderSentAt < :cutoff)")
    List<BorrowingRecord> findOverdueBorrowingsForReminder(@Param("cutoff") LocalDateTime cutoff);
}