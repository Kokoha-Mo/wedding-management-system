package com.wedding.wedding_management_system.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.repository.BookRepository;

@Service
@Transactional
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public List<Book> getBooksByCustomerId(int customerId) {
        return bookRepository.findByCustomer_Id(customerId);
    }

    public List<Book> getBooksByStatus(String status) {
        return bookRepository.findByStatus(status);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
}
