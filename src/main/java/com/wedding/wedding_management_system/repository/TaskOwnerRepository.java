package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wedding.wedding_management_system.entity.TaskOwner;

@Repository
public interface TaskOwnerRepository extends JpaRepository<TaskOwner, Integer> {

}
