package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Renders the HTML pages for Books (list / add / edit / delete).
 * The JSON REST API for books lives separately in BookController under /api/books.
 */
@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {

    private final BookService bookService;

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "books/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("book", new BookDTO());
        return "books/add";
    }

    @PostMapping("/add")
    public String addBook(@Valid @ModelAttribute("book") BookDTO book,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "books/add";
        }
        try {
            bookService.addBook(book);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "books/add";
        }
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Book book = bookService.getBookById(id);
        model.addAttribute("book", book);
        return "books/edit";
    }

    @PutMapping("/update/{id}")
    public String updateBook(@PathVariable Long id,
                              @Valid @ModelAttribute("book") BookDTO book,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("book", bookService.getBookById(id));
            return "books/edit";
        }
        try {
            bookService.updateBook(id, book);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("book", bookService.getBookById(id));
            return "books/edit";
        }
        return "redirect:/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "redirect:/books";
    }
}
