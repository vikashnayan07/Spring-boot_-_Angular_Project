package com.tcs.Machcare.service;

import com.tcs.Machcare.dto.RegisterRequest;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.entity.Login;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.repository.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 👉 NEW IMPORT
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

@Service
public class AuthService {

    @Autowired private EmployeeRepository empRepo;
    @Autowired private LoginRepository loginRepo;

    // 👉 1. Add the BCrypt Password Encoder
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<Employee> validateUser(String username, String password) {
        Optional<Login> loginOpt = loginRepo.findByUsername(username);
        
        if (loginOpt.isPresent()) {
            Login login = loginOpt.get();
            Employee emp = empRepo.findById(login.getEmpId())
                    .orElseThrow(() -> new RuntimeException("Employee record missing"));

            if (!emp.isActive()) {
                if (emp.getSuspensionEndDate() != null) {
                    if (LocalDateTime.now().isAfter(emp.getSuspensionEndDate())) {
                        emp.setActive(true);
                        emp.setSuspensionEndDate(null);
                        empRepo.save(emp);
                    } else {
                        throw new IllegalArgumentException("Account disabled until: " + emp.getSuspensionEndDate());
                    }
                } else {
                    throw new IllegalArgumentException("Account has been deactivated. Please contact your administrator.");
                }
            }

            // 👉 2. UPDATED: Mathematically verify the BCrypt hash instead of plain text!
            if (passwordEncoder.matches(password, login.getPassword())) {
                return Optional.of(emp);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void registerEmployee(RegisterRequest request) {
        Employee emp = new Employee();
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRoleId(request.getRole().getId());
        emp.setRoleName(request.getRole());
        emp.setActive(true); 
        emp.setSuspensionEndDate(null); 
        
       
     
        
        // 👉 3. NEW: Flag this new account as requiring setup!
        emp.setIsFirstLogin(true); 
        
        Employee savedEmp = empRepo.save(emp);
        
        Login login = new Login();
        login.setUsername(request.getEmail());          
        
        // 👉 4. UPDATED: Hash the password BEFORE saving it to the database
        login.setPassword(passwordEncoder.encode(request.getPassword()));
        
        login.setEmpId(savedEmp.getEmpId());
        loginRepo.save(login);
    }

    public void disableAccount(Long empId, int days) {
        Employee emp = empRepo.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        emp.setActive(false);
        emp.setSuspensionEndDate(LocalDateTime.now().plusDays(days));
        empRepo.save(emp);
    }

    // ==========================================
    // 👉 5. BRAND NEW: Handle First-Time Setup
    // ==========================================
    @Transactional
    public void setupFirstTimeAccount(Long empId, String newPassword, String q1, String a1, String q2, String a2) {
        
        // 1. Update the Login Table
        Login login = loginRepo.findByEmpId(empId)
            .orElseThrow(() -> new RuntimeException("Login credentials not found"));
            
        login.setPassword(passwordEncoder.encode(newPassword));
        login.setSecurityQuestion1(q1);
        login.setSecurityAnswer1(a1);
        login.setSecurityQuestion2(q2);
        login.setSecurityAnswer2(a2);
        loginRepo.save(login);

        // 2. Update the Employee Table (Turn off the setup flag forever!)
        Employee emp = empRepo.findById(empId)
            .orElseThrow(() -> new RuntimeException("Employee record not found"));
        
        emp.setIsFirstLogin(false);
        empRepo.save(emp);
    }
    
    
    
 // ==========================================
    // 👉 FORGOT PASSWORD FLOW
    // ==========================================
    
    public Map<String, String> getSecurityQuestions(String email) {
        Login login = loginRepo.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Account not found for this email."));
        
        // Prevent errors if they try to reset before ever doing the first-time setup
        if (login.getSecurityQuestion1() == null) {
            throw new RuntimeException("Security questions were never set up for this account.");
        }

        return Map.of(
            "q1", login.getSecurityQuestion1(),
            "q2", login.getSecurityQuestion2()
        );
    }

    @Transactional
    public void resetForgotPassword(String email, String a1, String a2, String newPassword) {
        Login login = loginRepo.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Account not found."));

        // Validate the answers (ignoring case and accidental spaces)
        boolean isA1Correct = login.getSecurityAnswer1().trim().equalsIgnoreCase(a1.trim());
        boolean isA2Correct = login.getSecurityAnswer2().trim().equalsIgnoreCase(a2.trim());

        if (!isA1Correct || !isA2Correct) {
            throw new RuntimeException("One or both security answers are incorrect.");
        }

        // Hash and save the new password
        login.setPassword(passwordEncoder.encode(newPassword));
        loginRepo.save(login);
    }
}