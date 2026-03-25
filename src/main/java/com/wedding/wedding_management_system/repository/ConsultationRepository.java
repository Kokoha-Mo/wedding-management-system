package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Consultation;
import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Integer> {
    
    // 透過方法名稱自動生成 SQL：SELECT * FROM consultation ORDER BY created_at DESC
    List<Consultation> findAllByOrderByCreatedAtDesc();
    
}