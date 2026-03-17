package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.BookDetail;

import java.util.List;

public interface BookDetailRepository extends JpaRepository<BookDetail, Integer> {
    List<BookDetail>findByBookId(Integer bookId);
    void deleteByBookId(Integer bookId);
}
