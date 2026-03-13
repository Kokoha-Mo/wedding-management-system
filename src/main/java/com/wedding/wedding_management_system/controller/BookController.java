package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping ("/Book")
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/cancelled")
    public ResponseEntity<List<Book>> getCancelledBooks() {
        List<Book> cancelledBooks = bookService.getCancelledBooks();
        return ResponseEntity.ok(cancelledBooks);
    }


}
