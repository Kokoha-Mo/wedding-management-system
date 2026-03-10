package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, Integer> {
}
