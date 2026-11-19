package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.dto.MemberDTO;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Verifies member registration and deletion rules without requiring a database.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MemberService memberService;

    @Test
    void registerMemberCreatesAnActiveMember() {
        MemberDTO request = validMemberRequest();
        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member created = memberService.registerMember(request);

        assertEquals("Robin", created.getFirstName());
        assertEquals("Mwaniki", created.getLastName());
        assertEquals(Member.MembershipStatus.ACTIVE, created.getStatus());
        verify(memberRepository).save(any(Member.class));
        verify(auditService).record(eq("CREATE"), eq("MEMBER"), any(), isNull(), anyString());
    }

    @Test
    void registerMemberRejectsAnEmailThatAlreadyExists() {
        MemberDTO request = validMemberRequest();
        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> memberService.registerMember(request));

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void deleteMemberRejectsMembersWithBorrowingHistory() {
        Member member = new Member();
        member.setId(12L);
        when(memberRepository.findById(12L)).thenReturn(java.util.Optional.of(member));
        when(borrowingRepository.existsByMemberId(12L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> memberService.deleteMember(12L));

        verify(memberRepository, never()).delete(any(Member.class));
    }

    @Test
    void deleteMemberRemovesMembersWithoutBorrowingHistory() {
        Member member = new Member();
        member.setId(13L);
        when(memberRepository.findById(13L)).thenReturn(java.util.Optional.of(member));
        when(borrowingRepository.existsByMemberId(13L)).thenReturn(false);

        memberService.deleteMember(13L);

        verify(memberRepository).delete(member);
    }


    @Test
    void searchMembersUsesDatabasePaginationAndPreservesSearchFilters() {
        Member member = new Member();
        member.setId(31L);
        member.setFirstName("Robin");
        Pageable pageable = PageRequest.of(1, 5);
        Page<Member> repositoryPage = new PageImpl<>(List.of(member), pageable, 11);
        when(memberRepository.searchMembers("robin", Member.MembershipStatus.ACTIVE, pageable))
                .thenReturn(repositoryPage);

        Page<Member> result = memberService.searchMembers(" robin ",
                Member.MembershipStatus.ACTIVE, pageable);

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(11, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(List.of(member), result.getContent());
        verify(memberRepository).searchMembers("robin", Member.MembershipStatus.ACTIVE, pageable);
    }

    @Test
    void searchMembersReturnsAnEmptyPageWhenRequestedPageIsOutOfRange() {
        Pageable pageable = PageRequest.of(4, 5);
        Page<Member> emptyPage = new PageImpl<>(List.of(), pageable, 11);
        when(memberRepository.searchMembers(isNull(), isNull(), eq(pageable))).thenReturn(emptyPage);

        Page<Member> result = memberService.searchMembers("", null, pageable);

        assertTrue(result.isEmpty());
        assertEquals(11, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }
    private MemberDTO validMemberRequest() {
        MemberDTO request = new MemberDTO();
        request.setFirstName("Robin");
        request.setLastName("Mwaniki");
        request.setEmail("robin@example.test");
        request.setPhone("+254700000000");
        request.setAddress("Nairobi");
        request.setMaxBooksAllowed(5);
        return request;
    }
}
