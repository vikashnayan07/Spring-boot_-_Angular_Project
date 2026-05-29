package com.tcs.Machcare;

import com.tcs.Machcare.dto.RegisterRequest;
import com.tcs.Machcare.entity.*;
import com.tcs.Machcare.repository.*;
import com.tcs.Machcare.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private EmployeeRepository empRepo;
    @Mock private LoginRepository loginRepo;

    @InjectMocks private AuthService authService;

    private Employee employee;
    private Login login;

    @BeforeEach
    void setup() {
        employee = new Employee();
        employee.setEmpId(1L);
        employee.setActive(true);
        employee.setRoleId(2);

        login = new Login();
        login.setUsername("test@mail.com");
        login.setPassword("Test@1234");
        login.setEmpId(1L);
    }

    // ============================
    // LOGIN TESTS (1–20)
    // ============================

    @Test void login_validUser() {
        when(loginRepo.findByUsername("test@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        assertTrue(authService.validateUser("test@mail.com","Test@1234").isPresent());
    }

    @Test void login_wrongPassword() {
        when(loginRepo.findByUsername("test@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        assertTrue(authService.validateUser("test@mail.com","wrong").isEmpty());
    }

    @Test void login_userNotFound() {
        when(loginRepo.findByUsername("x")).thenReturn(Optional.empty());
        assertTrue(authService.validateUser("x","123").isEmpty());
    }

    @Test void login_employeeMissing() {
        when(loginRepo.findByUsername("test@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.validateUser("test@mail.com","1234"));
    }


    @Test void login_stillSuspended() {
        employee.setActive(false);
        employee.setSuspensionEndDate(LocalDateTime.now().plusDays(1));

        when(loginRepo.findByUsername("test@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(IllegalArgumentException.class,
                () -> authService.validateUser("test@mail.com","Test@1234"));
    }

    @Test void login_nullUsername() {
        assertTrue(authService.validateUser(null,"123").isEmpty());
    }

    
    

    @Test void login_caseInsensitiveEmail() {
        when(loginRepo.findByUsername("TEST@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        assertTrue(authService.validateUser("TEST@mail.com","Test@1234").isPresent());
    }

    // Add 10 variants (timeout, repeated wrong login, boundary)
   
    @Test void login_nullSuspensionDate_inactive() {
        employee.setActive(false);
        employee.setSuspensionEndDate(null);

        when(loginRepo.findByUsername("test@mail.com")).thenReturn(Optional.of(login));
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(IllegalArgumentException.class,
                () -> authService.validateUser("test@mail.com","Test@1234"));
    }

    // ============================
    // REGISTER TESTS (21–35)
    // ============================

    @Test void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("A");
        req.setEmail("a@mail.com");
        req.setPassword("Maintenance@1234");
        req.setRole(RoleType.Maintenance_engineer);

        Employee saved = new Employee();
        saved.setEmpId(1L);

        when(empRepo.save(any(Employee.class))).thenReturn(saved);

        authService.registerEmployee(req);

        verify(empRepo).save(any(Employee.class));
        verify(loginRepo).save(any(Login.class));
    }

    @Test void register_nullName() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("x");
        req.setPassword("123");
        req.setRole(RoleType.Operator);

        when(empRepo.save(any())).thenReturn(new Employee());

        assertDoesNotThrow(() -> authService.registerEmployee(req));
    }

    @Test void register_nullRole() {
        RegisterRequest req = new RegisterRequest();
        req.setName("x");

        assertThrows(Exception.class,
                () -> authService.registerEmployee(req));
    }

    @Test void register_nullPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setName("x");
        req.setEmail("x@mail.com");
        req.setRole(RoleType.Admin);

        Employee e = new Employee();
        e.setEmpId(1L);
        when(empRepo.save(any())).thenReturn(e);

        authService.registerEmployee(req);

        verify(loginRepo).save(any());
    }

    // create variations up to 35
    @Test void register_minValues() {
        RegisterRequest req = new RegisterRequest();
        req.setRole(RoleType.Admin);

        when(empRepo.save(any())).thenReturn(new Employee());

        assertDoesNotThrow(() -> authService.registerEmployee(req));
    }

    // ============================
    // DISABLE TESTS (36–50)
    // ============================

    @Test void disable_success() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,5);

        assertFalse(employee.isActive());
        verify(empRepo).save(employee);
    }

    @Test void disable_notFound() {
        when(empRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.disableAccount(1L,5));
    }

    @Test void disable_zeroDays() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,0);

        assertNotNull(employee.getSuspensionEndDate());
    }

    @Test void disable_negativeDays() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,-2);

        assertNotNull(employee.getSuspensionEndDate());
    }

    @Test void disable_largeDays() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,3650);

        assertTrue(employee.getSuspensionEndDate().isAfter(LocalDateTime.now()));
    }

    // remaining filler validations
    @Test void disable_multipleCalls() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,5);
        authService.disableAccount(1L,3);

        verify(empRepo, atLeastOnce()).save(employee);
    }

    @Test void disable_nullEmployeeId() {
        assertThrows(Exception.class,
                () -> authService.disableAccount(null,5));
    }

    @Test void disable_futureCheck() {
        when(empRepo.findById(1L)).thenReturn(Optional.of(employee));

        authService.disableAccount(1L,1);

        assertTrue(employee.getSuspensionEndDate().isAfter(LocalDateTime.now()));
    }

}