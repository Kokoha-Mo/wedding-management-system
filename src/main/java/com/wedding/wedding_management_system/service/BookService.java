package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.entity.Book;
import java.util.List;

@Service
@Transactional
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    public List<Book> findByCustomerId(int customerId) {
        return bookRepository.findByCustomer_Id(customerId);
    }
}
