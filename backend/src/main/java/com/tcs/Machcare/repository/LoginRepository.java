package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {
    
    // REQUIRED by AuthService.validateUser()
    Optional<Login> findByUsername(String username);
    Optional<Login> findByEmpId(Long empId);
    void deleteByEmpId(Long empId);
}