package com.example.librarymanagementsystem.repository;



import com.example.librarymanagementsystem.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    List<Member> findByLastNameContainingIgnoreCase(String lastName);

    List<Member> findByStatus(Member.MembershipStatus status);

    boolean existsByEmail(String email);
}
