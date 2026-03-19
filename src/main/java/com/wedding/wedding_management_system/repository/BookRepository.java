package com.wedding.wedding_management_system.repository;

import java.util.List;
import java.util.Map;

import com.wedding.wedding_management_system.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.wedding_management_system.entity.Book;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByCustomerId(Integer customerId);

    List<Book> findByStatus(String status);

    List<Book> findByCustomer(Customer customer);

    Long countByStatus(String 處理中);

    List<Book> findByManager_IdAndStatus(Integer managerId , String status);

    long countByManager_IdAndStatus(Integer managerId, String status);





}


