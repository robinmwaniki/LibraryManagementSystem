package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.service.BookService;
import com.example.librarymanagementsystem.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies REST pagination input is bounded before it reaches repository-backed services.
 */
@ExtendWith(MockitoExtension.class)
class PaginationControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private MemberService memberService;

    @Test
    void bookEndpointClampsNegativePageAndExcessivePageSize() {
        BookController controller = new BookController(bookService);
        when(bookService.searchBooks(eq("spring"), eq("Technology"), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.getAllBooks("spring", "Technology", true, -3, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).searchBooks(eq("spring"), eq("Technology"), eq(true), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals("createdAt", pageable.getValue().getSort().iterator().next().getProperty());
    }

    @Test
    void memberEndpointClampsNegativePageAndExcessivePageSize() {
        MemberController controller = new MemberController(memberService);
        when(memberService.searchMembers(eq("robin"), eq(Member.MembershipStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.getAllMembers("robin", Member.MembershipStatus.ACTIVE, -1, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(memberService).searchMembers(eq("robin"), eq(Member.MembershipStatus.ACTIVE), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals("createdAt", pageable.getValue().getSort().iterator().next().getProperty());
    }
}
