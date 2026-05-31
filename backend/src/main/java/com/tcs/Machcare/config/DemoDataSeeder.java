package com.tcs.Machcare.config;

import com.tcs.Machcare.entity.AuditLog;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.Login;
import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.entity.MachineStatus;
import com.tcs.Machcare.entity.MaintenanceHistory;
import com.tcs.Machcare.entity.MaintenanceSchedule;
import com.tcs.Machcare.entity.MaintenanceStatus;
import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.PartUsage;
import com.tcs.Machcare.entity.Priority;
import com.tcs.Machcare.entity.RoleType;
import com.tcs.Machcare.entity.Severity;
import com.tcs.Machcare.repository.AuditLogRepository;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.repository.FaultLogRepository;
import com.tcs.Machcare.repository.LoginRepository;
import com.tcs.Machcare.repository.MachineAlertRepository;
import com.tcs.Machcare.repository.MachineRepository;
import com.tcs.Machcare.repository.MaintenanceHistoryRepository;
import com.tcs.Machcare.repository.MaintenanceScheduleRepository;
import com.tcs.Machcare.repository.PartRepository;
import com.tcs.Machcare.repository.PartUsageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Configuration
@Profile("prod")
public class DemoDataSeeder {

    private static final String DEMO_PASSWORD = "Admin@123";

    @Bean
    CommandLineRunner seedDemoData(
            EmployeeRepository employees,
            LoginRepository logins,
            MachineRepository machines,
            PartRepository parts,
            FaultLogRepository faults,
            MachineAlertRepository alerts,
            MaintenanceScheduleRepository schedules,
            MaintenanceHistoryRepository histories,
            PartUsageRepository usages,
            AuditLogRepository auditLogs,
            BCryptPasswordEncoder passwordEncoder
    ) {
        return args -> {
            Employee admin = ensureEmployee(employees, logins, passwordEncoder, "Aarav Sharma", "admin.demo@machcare.me", 1, RoleType.Admin);
            Employee engineer = ensureEmployee(employees, logins, passwordEncoder, "Neha Verma", "engineer.demo@machcare.me", 2, RoleType.Maintenance_engineer);
            Employee operator = ensureEmployee(employees, logins, passwordEncoder, "Rohan Mehta", "operator.demo@machcare.me", 3, RoleType.Operator);
            ensureEmployee(employees, logins, passwordEncoder, "Priya Nair", "engineer2.demo@machcare.me", 2, RoleType.Maintenance_engineer);
            ensureEmployee(employees, logins, passwordEncoder, "Kabir Sethi", "operator2.demo@machcare.me", 3, RoleType.Operator);

            seedMachines(machines);
            seedParts(parts);
            seedFaults(faults, operator);
            seedAlerts(alerts, engineer);
            seedSchedules(schedules, engineer);
            seedHistories(histories, engineer);
            seedUsages(usages, engineer);
            seedAuditLogs(auditLogs, admin, engineer, operator);
        };
    }

    private Employee ensureEmployee(
            EmployeeRepository employees,
            LoginRepository logins,
            BCryptPasswordEncoder passwordEncoder,
            String name,
            String email,
            int roleId,
            RoleType role
    ) {
        Employee employee = employees.findByEmail(email).orElseGet(Employee::new);
        employee.setName(name);
        employee.setEmail(email);
        employee.setRoleId(roleId);
        employee.setRoleName(role);
        employee.setActive(true);
        employee.setIsFirstLogin(false);
        employee = employees.save(employee);

        Login login = logins.findByUsername(email).orElseGet(Login::new);
        login.setUsername(email);
        login.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        login.setEmpId(employee.getEmpId());
        login.setSecurityQuestion1("Demo question 1");
        login.setSecurityAnswer1("demo");
        login.setSecurityQuestion2("Demo question 2");
        login.setSecurityAnswer2("demo");
        logins.save(login);
        return employee;
    }

    private void seedMachines(MachineRepository machines) {
        List<Machine> demoMachines = List.of(
                machine("MC-LATHE-01", "CNC Lathe Alpha", "CNC Lathe", MachineStatus.Running, "Healthy", "Active", "High", 72),
                machine("MC-PRESS-02", "Hydraulic Press Beta", "Hydraulic Press", MachineStatus.Idle, "Attention", "Active", "Critical", 58),
                machine("MC-CUT-03", "Laser Cutter Gamma", "Laser Cutter", MachineStatus.Running, "Healthy", "Active", "Medium", 81),
                machine("MC-COMP-04", "Air Compressor Delta", "Compressor", MachineStatus.Stopped, "At Risk", "Service Due", "High", 34),
                machine("MC-MILL-05", "Milling Center Sigma", "Milling Machine", MachineStatus.Running, "Healthy", "Active", "Medium", 66)
        );
        demoMachines.forEach(machine -> machines.findById(machine.getMachineId()).orElseGet(() -> machines.save(machine)));
    }

    private Machine machine(String id, String name, String type, MachineStatus status, String health, String lifecycle, String criticality, int load) {
        Machine machine = new Machine();
        machine.setMachineId(id);
        machine.setMachineName(name);
        machine.setMachineType(type);
        machine.setStatus(status);
        machine.setHealthStatus(health);
        machine.setLifecycleStatus(lifecycle);
        machine.setWarrantyStatus("In Warranty");
        machine.setProductionCriticality(criticality);
        machine.setCurrentOperationalLoad(load);
        machine.setProductionBottleneck("Critical".equals(criticality));
        machine.setExpectedMtbf(220 + load);
        machine.setExpectedMttr(90 + (100 - load));
        machine.setPurchaseDate(LocalDate.now().minusYears(2));
        machine.setWarrantyExpiryDate(LocalDate.now().plusMonths(18));
        machine.setLastServiceDate(LocalDate.now().minusDays(20));
        machine.setNextServiceDueDate(LocalDate.now().plusDays(10));
        machine.setDescription(type + " demo asset for maintenance workflows");
        return machine;
    }

    private void seedParts(PartRepository parts) {
        if (parts.count() >= 5) {
            return;
        }
        List<Part> demoParts = List.of(
                part("Servo Drive Module", "MC-LATHE-01", 4, 18),
                part("Hydraulic Seal Kit", "MC-PRESS-02", 10, 7),
                part("Laser Lens Assembly", "MC-CUT-03", 3, 11),
                part("Compressor Filter", "MC-COMP-04", 12, 24),
                part("Spindle Bearing Set", "MC-MILL-05", 5, 9)
        );
        parts.saveAll(demoParts);
    }

    private Part part(String name, String machineId, int minStock, int currentStock) {
        Part part = new Part();
        part.setPartName(name);
        part.setMachineId(machineId);
        part.setCategoryId(1L);
        part.setMinStock(minStock);
        part.setCurrentStock(currentStock);
        part.setManufactureDate(LocalDate.now().minusMonths(8));
        part.setExpiryDate(LocalDate.now().plusYears(2));
        part.setWarrantyExpiryDate(LocalDate.now().plusMonths(14));
        part.setShelfLifeDays(720);
        part.setConditionStatus(currentStock <= minStock ? "Low Stock" : "Good");
        part.setLifecycleStatus("Ready");
        return part;
    }

    private void seedFaults(FaultLogRepository faults, Employee operator) {
        List<FaultLog> demoFaults = List.of(
                fault("DEMO-FLT-001", "MC-PRESS-02", "Hydraulic pressure fluctuation detected", Severity.High, "High", operator),
                fault("DEMO-FLT-002", "MC-COMP-04", "Compressor outlet temperature above normal", Severity.Critical, "Critical", operator),
                fault("DEMO-FLT-003", "MC-LATHE-01", "Tool vibration warning during finishing pass", Severity.Medium, "Medium", operator),
                fault("DEMO-FLT-004", "MC-CUT-03", "Laser alignment drift noticed", Severity.Medium, "Medium", operator),
                fault("DEMO-FLT-005", "MC-MILL-05", "Coolant flow below target threshold", Severity.Low, "Low", operator)
        );
        demoFaults.forEach(fault -> faults.findById(fault.getFaultId()).orElseGet(() -> faults.save(fault)));
    }

    private FaultLog fault(String id, String machineId, String description, Severity severity, String priority, Employee operator) {
        FaultLog fault = new FaultLog();
        fault.setFaultId(id);
        fault.setMachineId(machineId);
        fault.setDescription(description);
        fault.setSeverity(severity);
        fault.setFaultDate(LocalDate.now().minusDays(Math.max(1, id.charAt(id.length() - 1) - '0')));
        fault.setFaultTime(LocalTime.of(9, 15));
        fault.setReportedBy(operator.getEmpId());
        fault.setReportedByName(operator.getName());
        fault.setPriorityScore(new BigDecimal(severity == Severity.Critical ? "92" : severity == Severity.High ? "78" : "54"));
        fault.setPriorityLevel(priority);
        fault.setProductionImpactScore(new BigDecimal(severity == Severity.Critical ? "88" : "61"));
        fault.setAnalysisStatus("Pending");
        return fault;
    }

    private void seedAlerts(MachineAlertRepository alerts, Employee engineer) {
        if (alerts.count() >= 5) {
            return;
        }
        List<MachineAlert> demoAlerts = List.of(
                alert("MC-PRESS-02", "Pressure instability", Severity.High, Priority._1, engineer, "DEMO-FLT-001"),
                alert("MC-COMP-04", "Thermal overload risk", Severity.Critical, Priority._1, engineer, "DEMO-FLT-002"),
                alert("MC-LATHE-01", "Vibration trend rising", Severity.Medium, Priority._2, engineer, "DEMO-FLT-003"),
                alert("MC-CUT-03", "Optical calibration required", Severity.Medium, Priority._2, engineer, "DEMO-FLT-004"),
                alert("MC-MILL-05", "Coolant system inspection", Severity.Low, Priority._3, engineer, "DEMO-FLT-005")
        );
        alerts.saveAll(demoAlerts);
    }

    private MachineAlert alert(String machineId, String issue, Severity severity, Priority priority, Employee engineer, String faultId) {
        MachineAlert alert = new MachineAlert();
        alert.setMachineId(machineId);
        alert.setIssueName(issue);
        alert.setSeverity(severity);
        alert.setPriority(priority);
        alert.setEmpId(engineer.getEmpId());
        alert.setAlertPriority(priority.getValue());
        alert.setAlertReason("Demo alert generated for dashboard presentation.");
        alert.setGeneratedBySystem(true);
        alert.setLinkedFaultId(faultId);
        return alert;
    }

    private void seedSchedules(MaintenanceScheduleRepository schedules, Employee engineer) {
        if (schedules.count() >= 5) {
            return;
        }
        schedules.saveAll(List.of(
                schedule("MC-PRESS-02", engineer, MaintenanceStatus.Pending, 1),
                schedule("MC-COMP-04", engineer, MaintenanceStatus.In_progress, 2),
                schedule("MC-LATHE-01", engineer, MaintenanceStatus.Pending, 3),
                schedule("MC-CUT-03", engineer, MaintenanceStatus.Completed, 4),
                schedule("MC-MILL-05", engineer, MaintenanceStatus.Pending, 5)
        ));
    }

    private MaintenanceSchedule schedule(String machineId, Employee engineer, MaintenanceStatus status, int offset) {
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setMachineId(machineId);
        schedule.setEmpId(engineer.getEmpId());
        schedule.setScheduleDate(LocalDate.now().plusDays(offset));
        schedule.setStatus(status);
        return schedule;
    }

    private void seedHistories(MaintenanceHistoryRepository histories, Employee engineer) {
        if (histories.count() >= 5) {
            return;
        }
        histories.saveAll(List.of(
                history("MC-CUT-03", engineer, MaintenanceStatus.Completed, "Lens cleaned and calibrated", 1L, 2),
                history("MC-LATHE-01", engineer, MaintenanceStatus.Completed, "Spindle vibration checked", 5L, 1),
                history("MC-PRESS-02", engineer, MaintenanceStatus.In_progress, "Hydraulic seals under inspection", 2L, 3),
                history("MC-COMP-04", engineer, MaintenanceStatus.Pending, "Thermal diagnostics queued", 4L, 1),
                history("MC-MILL-05", engineer, MaintenanceStatus.Completed, "Coolant pump flushed", 3L, 2)
        ));
    }

    private MaintenanceHistory history(String machineId, Employee engineer, MaintenanceStatus status, String remarks, Long partId, int qty) {
        MaintenanceHistory history = new MaintenanceHistory();
        history.setMachineId(machineId);
        history.setEmpId(engineer.getEmpId());
        history.setStatus(status);
        history.setRemarks(remarks);
        history.setMaintenanceDate(LocalDate.now().minusDays(7));
        history.setMaintenanceTime(LocalTime.of(10, 30));
        history.setResolvedDate(status == MaintenanceStatus.Completed ? LocalDate.now().minusDays(6) : null);
        history.setResolvedTime(status == MaintenanceStatus.Completed ? LocalTime.of(14, 0) : null);
        history.setPartId(partId);
        history.setQtyAssigned(qty);
        return history;
    }

    private void seedUsages(PartUsageRepository usages, Employee engineer) {
        if (usages.count() >= 5) {
            return;
        }
        for (long i = 1; i <= 5; i++) {
            PartUsage usage = new PartUsage();
            usage.setPartId(i);
            usage.setEmpId(engineer.getEmpId());
            usage.setQtyAssigned((int) i);
            usage.setScheduleId(i);
            usage.setLastUpdated(LocalDateTime.now().minusDays(i));
            usages.save(usage);
        }
    }

    private void seedAuditLogs(AuditLogRepository auditLogs, Employee admin, Employee engineer, Employee operator) {
        if (auditLogs.count() >= 5) {
            return;
        }
        List<Employee> actors = List.of(admin, engineer, operator, engineer, admin);
        List<String> actions = List.of("LOGIN", "ASSIGN_TASK", "REPORT_FAULT", "UPDATE_MAINTENANCE", "REVIEW_DASHBOARD");
        for (int i = 0; i < actors.size(); i++) {
            Employee actor = actors.get(i);
            AuditLog log = new AuditLog();
            log.setEmpId(actor.getEmpId());
            log.setEmpName(actor.getName());
            log.setEmail(actor.getEmail());
            log.setRoleId(actor.getRoleId());
            log.setAction(actions.get(i));
            log.setStatus("SUCCESS");
            log.setMessage("Demo audit event for presentation workflow.");
            log.setCreatedAt(LocalDateTime.now().minusHours(i + 1));
            auditLogs.save(log);
        }
    }
}
