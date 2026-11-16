package com.example.librarymanagementsystem.controller;

import com.example.librarymanagementsystem.dto.BookDTO;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {
    private final BookService bookService;

    @GetMapping
    public String listBooks(@RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "") String category,
                            @RequestParam(defaultValue = "false") boolean availableOnly,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 100);
        Page<Book> books = bookService.searchBooks(search, category, availableOnly,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("books", books);
        model.addAttribute("categories", bookService.getCategories());
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("availableOnly", availableOnly);
        model.addAttribute("pageSize", safeSize);
        return "books/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("book", new BookDTO());
        model.addAttribute("categories", bookService.getCategories());
        return "books/add";
    }

    @PostMapping("/add")
    public String addBook(@Valid @ModelAttribute("book") BookDTO book,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", bookService.getCategories());
            return "books/add";
        }
        try {
            bookService.addBook(book);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categories", bookService.getCategories());
            return "books/add";
        }
        redirectAttributes.addFlashAttribute("success", "Book added successfully.");
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id));
        model.addAttribute("categories", bookService.getCategories());
        return "books/edit";
    }

    @PutMapping("/update/{id}")
    public String updateBook(@PathVariable Long id, @Valid @ModelAttribute("book") BookDTO book,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", bookService.getCategories());
            return "books/edit";
        }
        try {
            bookService.updateBook(id, book);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categories", bookService.getCategories());
            return "books/edit";
        }
        redirectAttributes.addFlashAttribute("success", "Book updated successfully.");
        return "redirect:/books";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Book deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/books";
    }
}