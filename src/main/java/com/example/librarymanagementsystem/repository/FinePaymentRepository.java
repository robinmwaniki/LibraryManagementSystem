package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.FinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long> {
    List<FinePayment> findByBorrowingIdOrderByPaidAtAscIdAsc(Long borrowingId);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM FinePayment p WHERE p.borrowing.id = :borrowingId")
    BigDecimal sumByBorrowingId(@Param("borrowingId") Long borrowingId);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM FinePayment p WHERE p.borrowing.member.id = :memberId")
    BigDecimal sumByMemberId(@Param("memberId") Long memberId);
}