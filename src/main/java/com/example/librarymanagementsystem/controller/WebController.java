package com.example.librarymanagementsystem.controller;



import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.service.BookService;
import com.example.librarymanagementsystem.service.BorrowingService;
import com.example.librarymanagementsystem.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private BookService bookService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private BorrowingService borrowingService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Library Management System");

        // Get statistics for dashboard
        List<Book> allBooks = bookService.getAllBooks();
        List<Member> allMembers = memberService.getAllMembers();

        model.addAttribute("stats", new Object() {
            public final long totalBooks = allBooks.size();
            public final long activeMembers = allMembers.stream()
                    .filter(m -> m.getStatus() == Member.MembershipStatus.ACTIVE).count();
            public final long activeBorrowings = borrowingService.getAllBorrowings().stream()
                    .filter(b -> b.getStatus() == com.example.librarymanagementsystem.model.BorrowingRecord.BorrowStatus.BORROWED).count();
        });

        // Get recent books (last 5)
        List<Book> recentBooks = allBooks;
        if (recentBooks.size() > 5) {
            recentBooks = recentBooks.subList(recentBooks.size() - 5, recentBooks.size());
        }
        model.addAttribute("recentBooks", recentBooks);

        return "index";
    }
}