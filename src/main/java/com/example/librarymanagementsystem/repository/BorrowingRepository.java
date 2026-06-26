package com.example.librarymanagementsystem.repository;



import com.example.librarymanagementsystem.model.BorrowingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

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
}
