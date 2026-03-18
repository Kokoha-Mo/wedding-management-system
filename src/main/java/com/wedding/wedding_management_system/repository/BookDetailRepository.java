package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.BookDetail;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookDetailRepository extends JpaRepository<BookDetail, Integer> {
    List<BookDetail> findByBookIdOrderByServiceIdAsc(Integer bookId);

    @Modifying
    @Transactional
    void deleteByBookId(Integer bookId);
}
