package com.wedding.wedding_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.wedding_management_system.entity.Book;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByCustomer_Id(int customerId);

    List<Book> findByStatus(String status);

    List<Book> findByStatusOrderByCancel(String status);


}


