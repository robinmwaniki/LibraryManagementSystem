package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.model.BookReservation;
import com.example.librarymanagementsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Book Reservations", description = "Reservation and waiting-list management")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Reserve an available book or join its waiting list")
    public ResponseEntity<BookReservation> reserve(@RequestParam Long bookId, @RequestParam Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserveBook(bookId, memberId));
    }

    @PostMapping("/{reservationId}/collect")
    @Operation(summary = "Record collection of a ready reservation and create its loan")
    public ResponseEntity<BookReservation> collect(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.collectReservation(reservationId));
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Cancel a member reservation")
    public ResponseEntity<BookReservation> cancel(@PathVariable Long reservationId,
                                                    @RequestParam Long memberId) {
        return ResponseEntity.ok(reservationService.cancelReservation(reservationId, memberId));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "View reservation status for a member")
    public ResponseEntity<List<BookReservation>> memberReservations(@PathVariable Long memberId) {
        return ResponseEntity.ok(reservationService.getMemberReservations(memberId));
    }

    @GetMapping("/book/{bookId}/queue")
    @Operation(summary = "View reservation queue for a book")
    public ResponseEntity<List<BookReservation>> bookQueue(@PathVariable Long bookId) {
        return ResponseEntity.ok(reservationService.getBookQueue(bookId));
    }
}
