package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Employee;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByEmail(String email);

    @Query("SELECT e FROM Employee e " +
            "LEFT JOIN Book b ON b.manager = e " +
            "GROUP BY e " +
            "ORDER BY COUNT(b) ASC")
    List<Employee> findEmployeeWithLeastBooks();

    List<Employee> findByDepartment_Id(Integer deptId);
}
