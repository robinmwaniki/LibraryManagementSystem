package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.exception.ResourceNotFoundException;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.model.BookReservation.ReservationStatus;
import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.BookReservationRepository;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
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
    private final BorrowingRepository borrowingRepository;
    private final LibrarySettingsService librarySettingsService;

    @Value("${library.reservations.expiry-days:3}")
    private int expiryDays;

    public BookReservation reserveBook(Long bookId, Long memberId) {
        // Expire old holds before taking a title lock. This prevents a transaction
        // holding book A from trying to lock book B while another transaction does the reverse.
        expireReservations();

        // Serializes duplicate checks and queue allocation for this title.
        Book book = lockBook(bookId);
        expireReservationsForBook(bookId, LocalDateTime.now());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (member.getStatus() != Member.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only active members can reserve books.");
        }
        if (reservationRepository.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, ACTIVE_STATUSES)) {
            throw new IllegalStateException("This member already has an active reservation for the book.");
        }

        // Older waiting members always receive available stock before a new request.
        allocateWaitingReservations(bookId);
        BookReservation reservation = new BookReservation();
        reservation.setBook(book);
        reservation.setMember(member);

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
        BookReservation snapshot = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        Long bookId = snapshot.getBook().getId();
        lockBook(bookId);
        expireReservationsForBook(bookId, LocalDateTime.now());

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
            releaseReservedCopy(bookId);
        }
        return reservation;
    }

    public BookReservation collectReservation(Long reservationId) {
        BookReservation snapshot = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        Long bookId = snapshot.getBook().getId();
        lockBook(bookId);
        // A collection request is also an expiry boundary: do not leave an expired
        // hold blocking the next member merely because the hourly job has not run yet.
        expireReservationsForBook(bookId, LocalDateTime.now());

        BookReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.READY) {
            throw new IllegalStateException("Only reservations marked ready can be collected.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reservation.getExpiresAt() == null || !reservation.getExpiresAt().isAfter(now)) {
            throw new IllegalStateException("This reservation has expired and is no longer available for collection.");
        }

        Member member = memberRepository.findByIdForUpdate(reservation.getMember().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (member.getStatus() != Member.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only active members can collect reservations.");
        }
        long activeLoans = borrowingRepository.countActiveBorrowingsByMemberId(member.getId());
        int maximumAllowed = member.getMaxBooksAllowed() == null ? 5 : member.getMaxBooksAllowed();
        if (activeLoans >= maximumAllowed) {
            throw new IllegalStateException("Member has reached the maximum borrowing limit.");
        }

        int loanPeriodDays = librarySettingsService.getSettings().getLoanPeriodDays();
        BorrowingRecord loan = new BorrowingRecord();
        loan.setBook(reservation.getBook());
        loan.setMember(member);
        loan.setBorrowDate(now);
        loan.setDueDate(now.plusDays(loanPeriodDays));
        loan.setStatus(BorrowingRecord.BorrowStatus.BORROWED);
        borrowingRepository.save(loan);

        // The inventory was decremented when the reservation became READY.
        reservation.setStatus(ReservationStatus.COLLECTED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<BookReservation> getAllReservations() {
        return reservationRepository.findAllByOrderByCreatedAtAscIdAsc();
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
     * Reserves available copies for the oldest waiting members in deterministic FIFO order.
     * The caller must serialize this operation by locking the corresponding book row.
     */
    public void allocateWaitingReservations(Long bookId) {
        Book book = lockBook(bookId);
        boolean inventoryChanged = false;
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
            inventoryChanged = true;
        }
        if (inventoryChanged) {
            bookRepository.save(book);
        }
    }

    /**
     * Scheduled expiry releases each expired hold, then promotes the next FIFO member.
     * The book row is always locked before a reservation row to keep lock order consistent.
     */
    @Scheduled(cron = "${library.reservations.expiry-check-cron:0 0 * * * *}")
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<BookReservation> expiredCandidates = reservationRepository
                .findExpiredReadyReservations(ReservationStatus.READY, now);
        expiredCandidates.stream().map(r -> r.getBook().getId()).distinct()
                .forEach(bookId -> {
                    lockBook(bookId);
                    expireReservationsForBook(bookId, now);
                    allocateWaitingReservations(bookId);
                });
    }

    private void expireReservationsForBook(Long bookId, LocalDateTime now) {
        List<BookReservation> expired = reservationRepository
                .findExpiredReadyReservationsForBook(bookId, ReservationStatus.READY, now);
        if (expired.isEmpty()) return;

        Book book = lockBook(bookId);
        for (BookReservation candidate : expired) {
            BookReservation reservation = reservationRepository.findByIdForUpdate(candidate.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
            // Re-check after obtaining the row lock because collection/cancellation may have won the race.
            if (reservation.getStatus() != ReservationStatus.READY
                    || reservation.getExpiresAt() == null
                    || reservation.getExpiresAt().isAfter(now)) {
                continue;
            }
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            book.setAvailableCopies(safe(book.getAvailableCopies()) + 1);
        }
        bookRepository.save(book);
        allocateWaitingReservations(bookId);
    }

    private void releaseReservedCopy(Long bookId) {
        Book book = lockBook(bookId);
        book.setAvailableCopies(safe(book.getAvailableCopies()) + 1);
        bookRepository.save(book);
        allocateWaitingReservations(bookId);
    }

    private Book lockBook(Long bookId) {
        return bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    private int validatedExpiryDays() {
        return expiryDays < 1 ? 3 : Math.min(expiryDays, 30);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
