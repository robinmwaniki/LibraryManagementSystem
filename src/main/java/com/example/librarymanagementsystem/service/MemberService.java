package com.example.librarymanagementsystem.service;




import com.example.librarymanagementsystem.dto.MemberDTO;
import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.MemberRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;
    private AuditService auditService;

    @Autowired(required = false)
    public void setAuditService(AuditService auditService) { this.auditService = auditService; }

    public Member registerMember(MemberDTO memberDTO) {
        if (memberRepository.existsByEmail(memberDTO.getEmail())) {
            throw new RuntimeException("Member with email " + memberDTO.getEmail() + " already exists");
        }

        Member member = new Member();
        mapDTOToMember(memberDTO, member);
        member.setStatus(Member.MembershipStatus.ACTIVE);
        Member saved = memberRepository.save(member);
        audit("CREATE", saved.getId(), null, memberSummary(saved));
        return saved;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Member> searchMembers(String search, Member.MembershipStatus status, Pageable pageable) {
        String term = search == null || search.isBlank() ? null : search.trim();
        return memberRepository.searchMembers(term, status, pageable);
    }

    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
    }

    public Member updateMember(Long id, MemberDTO memberDTO) {
        Member member = getMemberById(id);
        String before = memberSummary(member);
        mapDTOToMember(memberDTO, member);
        Member saved = memberRepository.save(member);
        audit("UPDATE", id, before, memberSummary(saved));
        return saved;
    }

    public void deleteMember(Long id) {
        Member member = getMemberById(id);
        if (borrowingRepository.existsByMemberId(id)) {
            throw new IllegalStateException("This member has borrowing history and cannot be deleted. Change the membership status to INACTIVE instead.");
        }
        String before = memberSummary(member);
        memberRepository.delete(member);
        audit("DELETE", id, before, null);
    }

    public List<Member> searchMembersByName(String lastName) {
        return memberRepository.findByLastNameContainingIgnoreCase(lastName);
    }

    public void updateMemberStatus(Long id, String status) {
        Member member = getMemberById(id);
        String before = memberSummary(member);
        member.setStatus(Member.MembershipStatus.valueOf(status.toUpperCase()));
        Member saved = memberRepository.save(member);
        audit("STATUS_CHANGE", id, before, memberSummary(saved));
    }

    private void audit(String action, Long id, String before, String after) {
        if (auditService != null) auditService.record(action, "MEMBER", id, before, after);
    }

    /** Excludes names, email, phone and address from audit summaries. */
    private String memberSummary(Member member) {
        return "status=" + member.getStatus() + ",maxBooksAllowed=" + member.getMaxBooksAllowed();
    }

    private void mapDTOToMember(MemberDTO dto, Member member) {
        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setEmail(dto.getEmail());
        member.setPhone(dto.getPhone());
        member.setAddress(dto.getAddress());

        if (dto.getMaxBooksAllowed() != null) {
            member.setMaxBooksAllowed(dto.getMaxBooksAllowed());
        }
    }
}