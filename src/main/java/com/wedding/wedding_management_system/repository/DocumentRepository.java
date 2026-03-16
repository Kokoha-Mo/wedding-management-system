package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
}
