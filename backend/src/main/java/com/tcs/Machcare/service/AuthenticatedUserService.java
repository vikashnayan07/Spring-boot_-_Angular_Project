package com.tcs.Machcare.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.util.Jwtutil;

@Service
public class AuthenticatedUserService {

    private final Jwtutil jwtUtil;
    private final EmployeeRepository employeeRepository;

    public AuthenticatedUserService(Jwtutil jwtUtil, EmployeeRepository employeeRepository) {
        this.jwtUtil = jwtUtil;
        this.employeeRepository = employeeRepository;
    }

    public AuthenticatedUser requireRole(String token, int requiredRoleId, String message) {
        AuthenticatedUser user = fromToken(token);
        if (user.getRoleId() != requiredRoleId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return user;
    }

    public AuthenticatedUser fromToken(String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Integer roleId = jwtUtil.extractRoleId(token);
        String name = jwtUtil.extractName(token);

        if (name == null || name.trim().isEmpty()) {
            name = employeeRepository.findById(empId)
                    .map(Employee::getName)
                    .orElse("Unknown User");
        }

        return new AuthenticatedUser(empId, roleId == null ? 0 : roleId, name.trim());
    }

    public static class AuthenticatedUser {
        private final Long empId;
        private final int roleId;
        private final String name;

        public AuthenticatedUser(Long empId, int roleId, String name) {
            this.empId = empId;
            this.roleId = roleId;
            this.name = name;
        }

        public Long getEmpId() {
            return empId;
        }

        public int getRoleId() {
            return roleId;
        }

        public String getName() {
            return name;
        }
    }
}
