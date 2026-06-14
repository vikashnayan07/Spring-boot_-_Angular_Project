package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    // REQUIRED by MachCareCoreService.getLeastLoadedEngineer()
    List<Employee> findByRoleId(Integer roleId);

    @Query("select e from Employee e where e.roleId = :roleId and (e.isActive = true or e.isActive is null)")
    List<Employee> findByRoleIdAndIsActiveTrue(@Param("roleId") Integer roleId);

    Optional<Employee> findByEmail(String email);
}
