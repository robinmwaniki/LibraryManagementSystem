package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.FineAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface FineAdjustmentRepository extends JpaRepository<FineAdjustment, Long> {
    List<FineAdjustment> findByBorrowingIdOrderByAdjustedAtAscIdAsc(Long borrowingId);
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM FineAdjustment a WHERE a.borrowing.id = :borrowingId")
    BigDecimal sumByBorrowingId(@Param("borrowingId") Long borrowingId);
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM FineAdjustment a WHERE a.borrowing.member.id = :memberId")
    BigDecimal sumByMemberId(@Param("memberId") Long memberId);
}