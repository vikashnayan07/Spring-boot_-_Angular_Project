package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    // REQUIRED by MachCareCoreService.getLeastLoadedEngineer()
    List<Employee> findByRoleId(Integer roleId);

    List<Employee> findByRoleIdAndIsActiveTrue(Integer roleId);

    Optional<Employee> findByEmail(String email);
}