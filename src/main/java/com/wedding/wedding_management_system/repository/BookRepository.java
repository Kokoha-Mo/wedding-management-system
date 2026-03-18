package com.wedding.wedding_management_system.repository;

import java.util.List;

import com.wedding.wedding_management_system.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.wedding_management_system.entity.Book;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByCustomerId(Integer customerId);

    List<Book> findByStatus(String status);

    List<Book> findByCustomer(Customer customer);

    Long countByStatus(String 處理中);

    List<Book> findByStatusAndManagerIdOrderByCreateAtDesc(String status,Integer managerId);

    long countByManager_IdAndStatus(Integer managerId, String status);



}


