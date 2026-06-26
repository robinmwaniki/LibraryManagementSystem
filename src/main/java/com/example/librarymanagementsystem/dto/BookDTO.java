package com.example.librarymanagementsystem.dto;




import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String publisher;
    private Integer publicationYear;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Total copies is required")
    @Positive(message = "Total copies must be positive")
    private Integer totalCopies;

    private Integer availableCopies;
    private String shelfLocation;
}
