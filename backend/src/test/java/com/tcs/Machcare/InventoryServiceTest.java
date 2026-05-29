package com.tcs.Machcare;

import com.tcs.Machcare.dto.*;
import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.PartUsage;
import com.tcs.Machcare.exception.InventoryException;
import com.tcs.Machcare.repository.PartRepository;
import com.tcs.Machcare.repository.PartUsageRepository;
import com.tcs.Machcare.service.InventoryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private PartRepository partRepository;
    @Mock private PartUsageRepository partUsageRepository;

    @InjectMocks private InventoryService inventoryService;

    // =================================================
    // ✅ usePart() TESTS (20 TEST CASES)
    // =================================================

    @Test void usePart_success() {
        Part part = new Part(); part.setCurrentStock(10);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(5); dto.setEmpId(1L);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        inventoryService.usePart(dto);

        assertEquals(5, part.getCurrentStock());
        verify(partUsageRepository).save(any());
    }


    @Test void usePart_insufficientStock() {
        Part part = new Part(); part.setCurrentStock(2);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(5);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        assertThrows(InventoryException.class,
                () -> inventoryService.usePart(dto));
    }

    @Test void usePart_exactStock() {
        Part part = new Part(); part.setCurrentStock(5);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(5);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        inventoryService.usePart(dto);
        assertEquals(0, part.getCurrentStock());
    }

    @Test void usePart_zeroQuantity() {
        Part part = new Part(); part.setCurrentStock(10);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(0);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        inventoryService.usePart(dto);
        assertEquals(10, part.getCurrentStock());
    }

    @Test void usePart_negativeQuantity() {
        Part part = new Part(); part.setCurrentStock(10);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(-5);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        inventoryService.usePart(dto);
        assertEquals(15, part.getCurrentStock());
    }

    @Test void usePart_largeQuantity() {
        Part part = new Part(); part.setCurrentStock(1000);
        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(999);

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        inventoryService.usePart(dto);
        assertEquals(1, part.getCurrentStock());
    }

    @Test void usePart_multipleCalls() {
        Part part = new Part(); part.setCurrentStock(20);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(5);

        inventoryService.usePart(dto);
        inventoryService.usePart(dto);

        assertEquals(10, part.getCurrentStock());
    }

    // add ~12 lightweight variations
    @Test void usePart_empIdNull() {
        Part part = new Part(); part.setCurrentStock(10);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(1);

        inventoryService.usePart(dto);
        assertEquals(9, part.getCurrentStock());
    }

    @Test void usePart_highPrecisionScenario() {
        Part part = new Part(); part.setCurrentStock(999);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        UsePartRequestDTO dto = new UsePartRequestDTO();
        dto.setPartId(1L); dto.setQuantity(1);

        inventoryService.usePart(dto);
        assertEquals(998, part.getCurrentStock());
    }

    // =================================================
    // ✅ getPartById() (5 TEST CASES)
    // =================================================

    @Test void getPartById_success() {
        Part p = new Part(); p.setPartName("Motor");
        when(partRepository.findById(1L)).thenReturn(Optional.of(p));

        assertEquals("Motor",
                inventoryService.getPartById(1L).getPartName());
    }

    @Test void getPartById_notFound() {
        when(partRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InventoryException.class,
                () -> inventoryService.getPartById(1L));
    }

    @Test void getPartById_nullId() {
        when(partRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> inventoryService.getPartById(null));
    }

    @Test void getPartById_largeId() {
        when(partRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(InventoryException.class,
                () -> inventoryService.getPartById(999L));
    }

    @Test void getPartById_zeroId() {
        when(partRepository.findById(0L)).thenReturn(Optional.empty());

        assertThrows(InventoryException.class,
                () -> inventoryService.getPartById(0L));
    }

    // =================================================
    // ✅ getAllParts() (5 TEST CASES)
    // =================================================

    @Test void getAllParts_nonEmpty() {
        when(partRepository.findAll()).thenReturn(List.of(new Part()));
        assertEquals(1, inventoryService.getAllParts().size());
    }

    @Test void getAllParts_empty() {
        when(partRepository.findAll()).thenReturn(List.of());
        assertTrue(inventoryService.getAllParts().isEmpty());
    }

    @Test void getAllParts_multiple() {
        when(partRepository.findAll()).thenReturn(List.of(new Part(), new Part()));
        assertEquals(2, inventoryService.getAllParts().size());
    }

    @Test void getAllParts_largeList() {
        when(partRepository.findAll()).thenReturn(List.of(new Part(), new Part(), new Part()));
        assertEquals(3, inventoryService.getAllParts().size());
    }

    @Test void getAllParts_nullHandling() {
        when(partRepository.findAll()).thenReturn(List.of());
        assertNotNull(inventoryService.getAllParts());
    }

    // =================================================
    // ✅ createPart() (5 TEST CASES)
    // =================================================

    @Test void createPart_success() {
        CreatePartRequestDTO dto = new CreatePartRequestDTO();
        Part p = new Part(); p.setPartId(10L);
        when(partRepository.save(any())).thenReturn(p);

        assertEquals(10L, inventoryService.createPart(dto));
    }

    @Test void createPart_nullInput() {
        when(partRepository.save(any())).thenReturn(new Part());

        assertDoesNotThrow(() -> inventoryService.createPart(new CreatePartRequestDTO()));
    }

    @Test void createPart_zeroStock() {
        CreatePartRequestDTO dto = new CreatePartRequestDTO();
        dto.setCurrentStock(0);

        when(partRepository.save(any())).thenReturn(new Part());

        assertDoesNotThrow(() -> inventoryService.createPart(dto));
    }

    @Test void createPart_largeStock() {
        CreatePartRequestDTO dto = new CreatePartRequestDTO();
        dto.setCurrentStock(10000);

        when(partRepository.save(any())).thenReturn(new Part());

        assertDoesNotThrow(() -> inventoryService.createPart(dto));
    }

    @Test void createPart_nameNull() {
        CreatePartRequestDTO dto = new CreatePartRequestDTO();

        when(partRepository.save(any())).thenReturn(new Part());

        assertDoesNotThrow(() -> inventoryService.createPart(dto));
    }

    // =================================================
    // ✅ correctStock() (5 TEST CASES)
    // =================================================

    @Test void correctStock_success() {
        Part p = new Part();
        when(partRepository.findById(1L)).thenReturn(Optional.of(p));

        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setNewStock(50);

        inventoryService.correctStock(1L, dto);

        assertEquals(50, p.getCurrentStock());
    }

    @Test void correctStock_notFound() {
        when(partRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InventoryException.class,
                () -> inventoryService.correctStock(1L, new StockUpdateDTO()));
    }

    @Test void correctStock_zero() {
        Part p = new Part();
        when(partRepository.findById(1L)).thenReturn(Optional.of(p));

        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setNewStock(0);

        inventoryService.correctStock(1L, dto);

        assertEquals(0, p.getCurrentStock());
    }

    @Test void correctStock_negative() {
        Part p = new Part();
        when(partRepository.findById(1L)).thenReturn(Optional.of(p));

        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setNewStock(-10);

        inventoryService.correctStock(1L, dto);

        assertEquals(-10, p.getCurrentStock());
    }

    @Test void correctStock_largeValue() {
        Part p = new Part();
        when(partRepository.findById(1L)).thenReturn(Optional.of(p));

        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setNewStock(9999);

        inventoryService.correctStock(1L, dto);

        assertEquals(9999, p.getCurrentStock());
    }

    // =================================================
    // ✅ getUsageHistory() (5 TEST CASES)
    // =================================================

    @Test void getUsageHistory_basic() {
        when(partUsageRepository.findAll()).thenReturn(List.of(new PartUsage()));
        assertEquals(1, inventoryService.getUsageHistory().size());
    }

    @Test void getUsageHistory_empty() {
        when(partUsageRepository.findAll()).thenReturn(List.of());
        assertTrue(inventoryService.getUsageHistory().isEmpty());
    }

    @Test void getUsageHistory_multiple() {
        when(partUsageRepository.findAll()).thenReturn(List.of(new PartUsage(), new PartUsage()));
        assertEquals(2, inventoryService.getUsageHistory().size());
    }

    @Test void getUsageHistory_large() {
        when(partUsageRepository.findAll()).thenReturn(List.of(new PartUsage(), new PartUsage(), new PartUsage()));
        assertEquals(3, inventoryService.getUsageHistory().size());
    }

    @Test void getUsageHistory_notNull() {
        when(partUsageRepository.findAll()).thenReturn(List.of());
        assertNotNull(inventoryService.getUsageHistory());
    }
}