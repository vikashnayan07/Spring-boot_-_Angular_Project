package com.tcs.Machcare;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.tcs.Machcare.dto.FaultDTO;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.Severity;
import com.tcs.Machcare.exception.ResourceNotFoundException;
import com.tcs.Machcare.repository.FaultLogRepository;
import com.tcs.Machcare.service.FaultLogService;

@ExtendWith(MockitoExtension.class)
class FaultLogServiceTest {

    @Mock
    private FaultLogRepository repository;

    @InjectMocks
    private FaultLogService service;

    private FaultDTO dto;
    private FaultLog faultLog;

    private final Long empId = 1L;
    private final String empName = "Vikash";

    @BeforeEach
    void setup() {

        dto = new FaultDTO();
        dto.setMachineId("M001");
        dto.setDescription("Test fault");
        dto.setSeverity("HIGH");

        faultLog = new FaultLog();
        faultLog.setFaultId("F-1001");
        faultLog.setMachineId("M001");
        faultLog.setDescription("Test fault");
        faultLog.setSeverity(Severity.High);
        faultLog.setReportedBy(empId);
        faultLog.setReportedByName(empName);
        faultLog.setFaultDate(LocalDate.now());
        faultLog.setFaultTime(LocalTime.now().withNano(0));
    }

    // ================= POSITIVE TESTS =================

    @Test
    void createFault_success() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertNotNull(result);
        assertEquals("M001", result.getMachineId());
        assertEquals("Test fault", result.getDescription());
        assertEquals(Severity.High, result.getSeverity());
        assertEquals(empId, result.getReportedBy());
        assertEquals(empName, result.getReportedByName());

        verify(repository).save(any(FaultLog.class));
    }

    @Test
    void severity_shouldBeCaseInsensitive() {

        dto.setSeverity("high");

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertEquals(Severity.High, result.getSeverity());
    }

    @Test
    void faultId_shouldGenerateProperly() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertTrue(result.getFaultId().startsWith("F-"));
    }

    @Test
    void getAllFaultLogs_shouldReturnList() {

        when(repository.findAll()).thenReturn(List.of(faultLog));

        List<FaultLog> result = service.getAllFaultLogs();

        assertEquals(1, result.size());
    }

    @Test
    void getAllFaultLogs_shouldReturnEmptyList() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<FaultLog> result = service.getAllFaultLogs();

        assertTrue(result.isEmpty());
    }

    @Test
    void getFaultById_shouldReturnFault() {

        when(repository.findById("F-1001"))
                .thenReturn(Optional.of(faultLog));

        FaultLog result = service.getFaultById("F-1001");

        assertNotNull(result);
        assertEquals("F-1001", result.getFaultId());
    }

    @Test
    void createFault_shouldSetDate() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertEquals(LocalDate.now(), result.getFaultDate());
    }

    @Test
    void createFault_shouldSetTimeWithoutNano() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertEquals(0, result.getFaultTime().getNano());
    }

    // ================= NEGATIVE TESTS =================

    @Test
    void invalidSeverity_shouldThrowException() {

        dto.setSeverity("INVALID");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFault(dto, empId, empName)
        );
    }

    @Test
    void nullSeverity_shouldThrowException() {

        dto.setSeverity(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFault(dto, empId, empName)
        );
    }

    @Test
    void emptySeverity_shouldThrowException() {

        dto.setSeverity("");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFault(dto, empId, empName)
        );
    }

    @Test
    void getFaultById_notFound_shouldThrowException() {

        when(repository.findById("INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getFaultById("INVALID")
        );
    }

    @Test
    void repositorySaveThrowsException() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> service.createFault(dto, empId, empName)
        );
    }

    @Test
    void findAllThrowsException() {

        when(repository.findAll())
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> service.getAllFaultLogs()
        );
    }

    // ================= BOUNDARY TESTS =================

    @Test
    void longDescription_shouldWork() {

        dto.setDescription("A".repeat(500));

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertEquals(500, result.getDescription().length());
    }

    @Test
    void nullDescription_shouldWork() {

        dto.setDescription(null);

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertNull(result.getDescription());
    }
    
    @Test
    void criticalSeverity_shouldWork() {

        dto.setSeverity("CRITICAL");

        when(repository.findAll()).thenReturn(Collections.emptyList());

        when(repository.save(any(FaultLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FaultLog result = service.createFault(dto, empId, empName);

        assertEquals(Severity.Critical, result.getSeverity());
    }
}