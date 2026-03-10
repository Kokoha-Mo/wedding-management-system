package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
