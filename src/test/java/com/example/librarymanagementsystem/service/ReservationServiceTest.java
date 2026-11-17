package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.model.BookReservation.ReservationStatus;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BookReservationRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock private BookReservationRepository reservationRepository;
    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @InjectMocks private ReservationService reservationService;

    @Test
    void reservesAvailableCopyImmediatelyAndHoldsInventory() {
        Book book = book(1L, 2);
        Member member = member(2L);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(i -> i.getArgument(0));

        BookReservation reservation = reservationService.reserveBook(1L, 2L);

        assertEquals(ReservationStatus.READY, reservation.getStatus());
        assertNotNull(reservation.getExpiresAt());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void queuesReservationWhenNoCopiesAreAvailable() {
        Book book = book(1L, 0);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member(2L)));
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(i -> i.getArgument(0));

        BookReservation reservation = reservationService.reserveBook(1L, 2L);

        assertEquals(ReservationStatus.WAITING, reservation.getStatus());
        assertNull(reservation.getExpiresAt());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void preventsDuplicateActiveReservationForSameMemberAndBook() {
        Book book = book(1L, 0);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member(2L)));
        when(reservationRepository.existsByBookIdAndMemberIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reservationService.reserveBook(1L, 2L));
        verify(reservationRepository, never()).save(any(BookReservation.class));
    }

    @Test
    void cancellationReleasesReadyCopyAndServesNextWaitingMember() {
        Book book = book(1L, 0);
        BookReservation ready = reservation(11L, book, member(2L), ReservationStatus.READY);
        BookReservation waiting = reservation(12L, book, member(3L), ReservationStatus.WAITING);
        when(reservationRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(ready));
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(reservationRepository.findByBookIdAndStatusOrderByCreatedAtAscIdAsc(1L, ReservationStatus.WAITING))
                .thenReturn(List.of(waiting), List.of());
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(i -> i.getArgument(0));

        BookReservation cancelled = reservationService.cancelReservation(11L, 2L);

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(ReservationStatus.READY, waiting.getStatus());
        assertNotNull(waiting.getExpiresAt());
        assertEquals(0, book.getAvailableCopies());
    }

    private Book book(Long id, int available) {
        Book book = new Book();
        book.setId(id);
        book.setTotalCopies(2);
        book.setAvailableCopies(available);
        return book;
    }

    private Member member(Long id) {
        Member member = new Member();
        member.setId(id);
        member.setStatus(Member.MembershipStatus.ACTIVE);
        return member;
    }

    private BookReservation reservation(Long id, Book book, Member member, ReservationStatus status) {
        BookReservation reservation = new BookReservation();
        reservation.setId(id);
        reservation.setBook(book);
        reservation.setMember(member);
        reservation.setStatus(status);
        return reservation;
    }
}
