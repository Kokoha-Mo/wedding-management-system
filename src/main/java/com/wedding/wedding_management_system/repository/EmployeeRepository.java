package com.wedding.wedding_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.wedding.wedding_management_system.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByEmail(String email);

    @Query("SELECT e FROM Employee e "
            + "LEFT JOIN Book b ON b.manager = e "
            + "GROUP BY e "
            + "ORDER BY COUNT(b) ASC")
    List<Employee> findEmployeeWithLeastBooks();

    List<Employee> findByDepartmentId(Integer deptId);
}
