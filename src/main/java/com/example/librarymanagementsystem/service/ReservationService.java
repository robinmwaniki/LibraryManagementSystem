package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.model.BookReservation.ReservationStatus;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BookReservationRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final BookReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Value("${library.reservations.expiry-days:3}")
    private int expiryDays;

    public BookReservation reserveBook(Long bookId, Long memberId) {
        // Serialize reservation creation per book so duplicate checks and queue positions are race-safe.
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (member.getStatus() != Member.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only active members can reserve books.");
        }

        expireReservations();
        if (reservationRepository.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, ACTIVE_STATUSES)) {
            throw new IllegalStateException("This member already has an active reservation for the book.");
        }

        BookReservation reservation = new BookReservation();
        reservation.setBook(book);
        reservation.setMember(member);

        // If stock is available, reserve a copy immediately; otherwise join the FIFO queue.
        if (safe(book.getAvailableCopies()) > 0) {
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            bookRepository.save(book);
            reservation.setStatus(ReservationStatus.READY);
            reservation.setReadyAt(LocalDateTime.now());
            reservation.setExpiresAt(LocalDateTime.now().plusDays(validatedExpiryDays()));
        } else {
            reservation.setStatus(ReservationStatus.WAITING);
        }
        return reservationRepository.save(reservation);
    }

    public BookReservation cancelReservation(Long reservationId, Long memberId) {
        BookReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        if (!reservation.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("Reservation does not belong to this member.");
        }
        if (!ACTIVE_STATUSES.contains(reservation.getStatus())) {
            throw new IllegalStateException("Only waiting or ready reservations can be cancelled.");
        }

        boolean wasReady = reservation.getStatus() == ReservationStatus.READY;
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        if (wasReady) {
            releaseReservedCopy(reservation.getBook().getId());
        }
        return reservation;
    }

    @Transactional(readOnly = true)
    public List<BookReservation> getMemberReservations(Long memberId) {
        return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<BookReservation> getBookQueue(Long bookId) {
        return reservationRepository.findByBookIdOrderByCreatedAtAscIdAsc(bookId);
    }

    /**
     * Called in the return transaction after a copy is added back to inventory.
     * The oldest waiting member receives the copy before it becomes generally available.
     */
    public void allocateWaitingReservations(Long bookId) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        while (safe(book.getAvailableCopies()) > 0) {
            List<BookReservation> waiting = reservationRepository
                    .findByBookIdAndStatusOrderByCreatedAtAscIdAsc(bookId, ReservationStatus.WAITING);
            if (waiting.isEmpty()) break;
            BookReservation next = waiting.get(0);
            next.setStatus(ReservationStatus.READY);
            next.setReadyAt(LocalDateTime.now());
            next.setExpiresAt(LocalDateTime.now().plusDays(validatedExpiryDays()));
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            reservationRepository.save(next);
        }
        bookRepository.save(book);
    }

    /**
     * Hourly expiry releases unclaimed copies and immediately offers them to the next queue entries.
     */
    @Scheduled(cron = "${library.reservations.expiry-check-cron:0 0 * * * *}")
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<BookReservation> expired = reservationRepository
                .findExpiredReadyReservations(ReservationStatus.READY, now);
        for (BookReservation reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            Book book = bookRepository.findByIdForUpdate(reservation.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
            book.setAvailableCopies(safe(book.getAvailableCopies()) + 1);
            bookRepository.save(book);
        }
        expired.stream().map(r -> r.getBook().getId()).distinct()
                .forEach(this::allocateWaitingReservations);
    }

    private void releaseReservedCopy(Long bookId) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        book.setAvailableCopies(safe(book.getAvailableCopies()) + 1);
        bookRepository.save(book);
        allocateWaitingReservations(bookId);
    }

    private int validatedExpiryDays() {
        return expiryDays < 1 ? 3 : Math.min(expiryDays, 30);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
