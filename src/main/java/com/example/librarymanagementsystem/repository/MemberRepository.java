package com.example.librarymanagementsystem.repository;

import com.example.librarymanagementsystem.model.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByLastNameContainingIgnoreCase(String lastName);
    List<Member> findByStatus(Member.MembershipStatus status);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    long countByStatus(Member.MembershipStatus status);

    @Query("""
        SELECT m FROM Member m
        WHERE (:term IS NULL OR :term = ''
            OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :term, '%')))
          AND (:status IS NULL OR m.status = :status)
        """)
    Page<Member> searchMembers(@Param("term") String term,
                               @Param("status") Member.MembershipStatus status,
                               Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}