package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.model.BookReservation.ReservationStatus;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.LibrarySettings;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BookReservationRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    @Mock private BorrowingRepository borrowingRepository;
    @Mock private LibrarySettingsService librarySettingsService;
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
        assertNotNull(reservation.getReadyAt());
        assertNotNull(reservation.getExpiresAt());
        assertEquals(1, book.getAvailableCopies());
        verify(bookRepository).save(book);
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
        when(reservationRepository.findById(11L)).thenReturn(Optional.of(ready));
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
        verify(bookRepository, atLeastOnce()).save(book);
    }

    @Test
    void expiresReadyReservationAndPromotesNextMemberInFifoOrder() {
        Book book = book(1L, 0);
        BookReservation expired = reservation(21L, book, member(2L), ReservationStatus.READY);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        BookReservation waiting = reservation(22L, book, member(3L), ReservationStatus.WAITING);
        when(reservationRepository.findExpiredReadyReservations(eq(ReservationStatus.READY), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));
        when(reservationRepository.findExpiredReadyReservationsForBook(eq(1L), eq(ReservationStatus.READY),
                any(LocalDateTime.class))).thenReturn(List.of(expired));
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(reservationRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(expired));
        when(reservationRepository.findByBookIdAndStatusOrderByCreatedAtAscIdAsc(1L, ReservationStatus.WAITING))
                .thenReturn(List.of(waiting), List.of());
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(i -> i.getArgument(0));

        reservationService.expireReservations();

        assertEquals(ReservationStatus.EXPIRED, expired.getStatus());
        assertEquals(ReservationStatus.READY, waiting.getStatus());
        assertNotNull(waiting.getExpiresAt());
        assertEquals(0, book.getAvailableCopies());
    }

    @Test
    void collectionCreatesLoanAndMarksReservationCollected() {
        Book book = book(1L, 0);
        Member member = member(2L);
        BookReservation ready = reservation(31L, book, member, ReservationStatus.READY);
        ready.setExpiresAt(LocalDateTime.now().plusDays(1));
        LibrarySettings settings = new LibrarySettings();
        settings.setLoanPeriodDays(14);

        when(reservationRepository.findById(31L)).thenReturn(Optional.of(ready));
        when(reservationRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(ready));
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(member));
        when(borrowingRepository.countActiveBorrowingsByMemberId(2L)).thenReturn(0L);
        when(librarySettingsService.getSettings()).thenReturn(settings);
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(i -> i.getArgument(0));

        BookReservation collected = reservationService.collectReservation(31L);

        assertEquals(ReservationStatus.COLLECTED, collected.getStatus());
        verify(borrowingRepository).save(argThat(loan ->
                loan.getBook().getId().equals(1L)
                        && loan.getMember().getId().equals(2L)
                        && loan.getDueDate().isAfter(loan.getBorrowDate())));
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
        member.setMaxBooksAllowed(5);
        return member;
    }

    private BookReservation reservation(Long id, Book book, Member member, ReservationStatus status) {
        BookReservation reservation = new BookReservation();
        reservation.setId(id);
        reservation.setBook(book);
        reservation.setMember(member);
        reservation.setStatus(status);
        reservation.setCreatedAt(LocalDateTime.now().minusMinutes(id));
        return reservation;
    }
}
