package com.wedding.wedding_management_system.repository;

import java.util.List;
import java.util.Map;

import com.wedding.wedding_management_system.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.wedding_management_system.entity.Book;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByCustomerId(Integer customerId);

    List<Book> findByStatus(String status);

    List<Book> findByCustomer(Customer customer);

    Long countByStatus(String status);

    List<Book> findByManager_IdAndStatus(Integer managerId , String status);

    long countByManager_IdAndStatus(Integer managerId, String status);

    @Query("SELECT b FROM Book b " +
            "LEFT JOIN b.customer c " +
            "LEFT JOIN b.manager m " +
            "WHERE b.status = :status " +
            "AND (:managerId IS NULL OR m.id = :managerId) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY b.id DESC")
    List<Book> findByConditions(
            @Param("managerId") Integer managerId,
            @Param("status") String status,
            @Param("keyword") String keyword
       );
}


