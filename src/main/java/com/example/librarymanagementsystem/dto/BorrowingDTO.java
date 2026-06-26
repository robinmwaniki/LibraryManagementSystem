package com.example.librarymanagementsystem.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long memberId;
    private String memberName;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;
    private Double fine;
}