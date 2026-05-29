//package com.tcs.machcare.service;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import com.tcs.machcare.entity.FaultAnalysis;
//import com.tcs.machcare.exception.ResourceNotFoundException;
//import com.tcs.machcare.repository.FaultAnalysisRepository;
//
//public class FaultAnalysisServiceTest {
//
//    @Mock
//    private FaultAnalysisRepository repository;
//
//    @InjectMocks
//    private FaultAnalysisService service;
//
//    @BeforeEach
//    void setup() {
//
//        MockitoAnnotations.openMocks(this);
//    }
//
//    // 1
//    @Test
//    void testGetAllAnalysesSuccess() {
//
//        FaultAnalysis a1 =
//                new FaultAnalysis();
//
//        FaultAnalysis a2 =
//                new FaultAnalysis();
//
//        when(repository.findAll())
//                .thenReturn(
//                        Arrays.asList(a1, a2)
//                );
//
//        List<FaultAnalysis> list =
//                service.getAllAnalyses();
//
//        assertEquals(2, list.size());
//    }
//
//    // 2
//    @Test
//    void testGetAllAnalysesEmpty() {
//
//        when(repository.findAll())
//                .thenReturn(
//                        new ArrayList<>()
//                );
//
//        List<FaultAnalysis> list =
//                service.getAllAnalyses();
//
//        assertTrue(list.isEmpty());
//    }
//
//    // 3
//    @Test
//    void testGetAnalysisByIdSuccess() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setAnalysisId(1L);
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                1L,
//                result.getAnalysisId()
//        );
//    }
//
//    // 4
//    @Test
//    void testGetAnalysisByIdNotFound() {
//
//        when(repository.findById(1L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(
//                ResourceNotFoundException.class,
//                () -> service.getAnalysisById(1L)
//        );
//    }
//
//    // 5
//    @Test
//    void testRepositoryFindAllCalled() {
//
//        when(repository.findAll())
//                .thenReturn(
//                        new ArrayList<>()
//                );
//
//        service.getAllAnalyses();
//
//        verify(repository, times(1))
//                .findAll();
//    }
//
//    // 6
//    @Test
//    void testRepositoryFindByIdCalled() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        service.getAnalysisById(1L);
//
//        verify(repository, times(1))
//                .findById(1L);
//    }
//
//    // 7
//    @Test
//    void testFaultIdMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setFaultId("F-100");
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                "F-100",
//                result.getFaultId()
//        );
//    }
//
//    // 8
//    @Test
//    void testPriorityMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setPriority("1");
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                "1",
//                result.getPriority()
//        );
//    }
//
//    // 9
//    @Test
//    void testDescriptionMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setDescription(
//                "Oil leakage analysis"
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                "Oil leakage analysis",
//                result.getDescription()
//        );
//    }
//
//    // 10
//    @Test
//    void testHealthScoreMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setHealthScore(
//                new BigDecimal("75.00")
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                new BigDecimal("75.00"),
//                result.getHealthScore()
//        );
//    }
//
//    // 11
//    @Test
//    void testRiskScoreMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setRiskScore(
//                new BigDecimal("40.00")
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                new BigDecimal("40.00"),
//                result.getRiskScore()
//        );
//    }
//
//    // 12
//    @Test
//    void testTotalFailuresMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setTotalFailures(5);
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                5,
//                result.getTotalFailures()
//        );
//    }
//
//    // 13
//    @Test
//    void testMtbfHoursMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setMtbfHours(
//                new BigDecimal("12.50")
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                new BigDecimal("12.50"),
//                result.getMtbfHours()
//        );
//    }
//
//    // 14
//    @Test
//    void testFailureFrequencyMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setFailureFrequency(
//                new BigDecimal("1.20")
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                new BigDecimal("1.20"),
//                result.getFailureFrequency()
//        );
//    }
//
//    // 15
//    @Test
//    void testMttrMapping() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setMttr(
//                new BigDecimal("3.50")
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                new BigDecimal("3.50"),
//                result.getMttr()
//        );
//    }
//
//    // 16
//    @Test
//    void testNullFaultId() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setFaultId(null);
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertNull(result.getFaultId());
//    }
//
//    // 17
//    @Test
//    void testEmptyDescription() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setDescription("");
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertEquals(
//                "",
//                result.getDescription()
//        );
//    }
//
//    // 18
//    @Test
//    void testLargeDescription() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setDescription(
//                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
//        );
//
//        when(repository.findById(1L))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(1L);
//
//        assertNotNull(
//                result.getDescription()
//        );
//    }
//
//    // 19
//    @Test
//    void testSingleAnalysisInList() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        when(repository.findAll())
//                .thenReturn(
//                        Arrays.asList(analysis)
//                );
//
//        List<FaultAnalysis> list =
//                service.getAllAnalyses();
//
//        assertEquals(
//                1,
//                list.size()
//        );
//    }
//
//    // 20
//    @Test
//    void testAnalysisIdBoundary() {
//
//        FaultAnalysis analysis =
//                new FaultAnalysis();
//
//        analysis.setAnalysisId(Long.MAX_VALUE);
//
//        when(repository.findById(Long.MAX_VALUE))
//                .thenReturn(
//                        Optional.of(analysis)
//                );
//
//        FaultAnalysis result =
//                service.getAnalysisById(Long.MAX_VALUE);
//
//        assertEquals(
//                Long.MAX_VALUE,
//                result.getAnalysisId()
//        );
//    }
//}
package com.tcs.Machcare;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import com.tcs.Machcare.entity.*;
import com.tcs.Machcare.repository.*;
import com.tcs.Machcare.service.FaultAnalysisService;

class FaultAnalysisServiceTest {

    @Mock
    private FaultAnalysisRepository analysisRepo;

    @Mock
    private FaultLogRepository faultRepo;

    @Mock
    private MaintenanceHistoryRepository historyRepo;

    @InjectMocks
    private FaultAnalysisService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ✅ Helper method
    private FaultLog buildFault(String severity) {
        FaultLog fault = new FaultLog();
        fault.setFaultId("F1");
        fault.setMachineId("M001");
        fault.setFaultDate(LocalDate.now());
        fault.setFaultTime(LocalTime.of(10, 0));
        fault.setSeverity(Severity.valueOf(severity));
        return fault;
    }

    private MaintenanceHistory buildHistory(int hours) {
        MaintenanceHistory history = new MaintenanceHistory();
        history.setMachineId("M001");
        history.setResolvedDate(LocalDate.now());
        history.setResolvedTime(LocalTime.of(10 + hours, 0));
        return history;
    }

    // ===============================
    // ✅ ✅ POSITIVE TEST CASES (8)
    // ===============================

   
    @Test
    void testGenerateAnalysis_LowSeverity_FastMTTR() {
        FaultLog fault = buildFault("Low");
        MaintenanceHistory history = buildHistory(1);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertEquals("2", result.getPriority());
    }

    @Test
    void testGenerateAnalysis_MediumSeverity() {
        FaultLog fault = buildFault("Medium");
        MaintenanceHistory history = buildHistory(2);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertNotNull(result);
    }

    @Test
    void testGenerateAnalysis_CriticalSeverity() {
        FaultLog fault = buildFault("Critical");
        MaintenanceHistory history = buildHistory(2);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertEquals("1", result.getPriority());
    }

    @Test
    void testGenerateAnalysis_WithMultipleFailures() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault, fault, fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Collections.emptyList());
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertTrue(result.getTotalFailures() >= 3);
    }

    @Test
    void testGenerateAnalysis_NoMaintenanceHistory() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Collections.emptyList());
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertEquals(BigDecimal.ZERO, result.getMttr());
    }

    @Test
    void testGenerateAnalysis_HealthScoreNonNegative() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault, fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Collections.emptyList());
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertTrue(result.getHealthScore().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testGenerateAnalysis_SaveCalled() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Collections.emptyList());
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.generateAnalysis("F1");

        verify(analysisRepo, times(1)).save(any());
    }

    // ===============================
    // ❌ NEGATIVE TEST CASES (8)
    // ===============================

    @Test
    void testFaultNotFound() {
        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> service.generateAnalysis("F1"));
    }

    @Test
    void testNullMachineId() {
        FaultLog fault = buildFault("High");
        fault.setMachineId(null);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));

        assertDoesNotThrow(() -> service.generateAnalysis("F1"));
    }

    @Test
    void testNullMaintenanceHistoryList() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(null);
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.generateAnalysis("F1"));
    }

    @Test
    void testNullResolvedTime() {
        FaultLog fault = buildFault("High");
        MaintenanceHistory history = new MaintenanceHistory();

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertEquals(BigDecimal.ZERO, result.getMttr());
    }

    @Test
    void testNegativeMTTRScenario() {
        FaultLog fault = buildFault("High");
        MaintenanceHistory history = buildHistory(-1);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.generateAnalysis("F1"));
    }

    @Test
    void testEmptyFaultList() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Collections.emptyList());
        when(historyRepo.findByMachineId("M001")).thenReturn(Collections.emptyList());
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.generateAnalysis("F1"));
    }

    @Test
    void testSeverityNull() {
        FaultLog fault = buildFault("High");
        fault.setSeverity(null);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));

        assertThrows(Exception.class, () -> service.generateAnalysis("F1"));
    }

    @Test
    void testRepositorySaveFailure() {
        FaultLog fault = buildFault("High");

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(analysisRepo.save(any())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> service.generateAnalysis("F1"));
    }

    // ===============================
    // ⚡ BOUNDARY TEST CASES (4)
    // ===============================

    @Test
    void testBoundary_MTTR_60() {
        FaultLog fault = buildFault("High");
        MaintenanceHistory history = buildHistory(1); // 60 min

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertNotNull(result);
    }

    @Test
    void testBoundary_MTTR_120() {
        FaultLog fault = buildFault("High");
        MaintenanceHistory history = buildHistory(2);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.generateAnalysis("F1"));
    }

    @Test
    void testBoundary_MTTR_180() {
        FaultLog fault = buildFault("High");
        MaintenanceHistory history = buildHistory(3);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.generateAnalysis("F1"));
    }

    @Test
    void testBoundary_TotalScore_66() {
        FaultLog fault = buildFault("Medium");
        MaintenanceHistory history = buildHistory(2);

        when(faultRepo.findById("F1")).thenReturn(java.util.Optional.of(fault));
        when(faultRepo.findAll()).thenReturn(Arrays.asList(fault));
        when(historyRepo.findByMachineId("M001")).thenReturn(Arrays.asList(history));
        when(analysisRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FaultAnalysis result = service.generateAnalysis("F1");

        assertEquals("2", result.getPriority());
    }
}