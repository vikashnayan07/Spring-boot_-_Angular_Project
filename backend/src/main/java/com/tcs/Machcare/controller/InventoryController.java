package com.tcs.Machcare.controller;

import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.PartUsage;
import com.tcs.Machcare.repository.PartRepository;
import com.tcs.Machcare.repository.PartUsageRepository;
import com.tcs.Machcare.exception.InventoryException;
import com.tcs.Machcare.dto.*;
import com.tcs.Machcare.util.Jwtutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//  CHANGED: Now it is a real Web Controller!
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired private PartRepository partRepository;
    @Autowired private PartUsageRepository partUsageRepository;
    
    // NEW: Injected Jwtutil for Role-Based Security
    @Autowired private Jwtutil jwtUtil; 

    // ==========================================
    


    // ==========================================
    // 3. USE PART (Engineers & Admins)
    // ==========================================
   

    // ==========================================
    // 4. GENERAL VIEWS (Open to all authenticated users)
    // ==========================================
    @GetMapping("/parts/{id}")
    public ResponseEntity<PartDTO> getPartById(@PathVariable Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new InventoryException("Part not found"));
        return ResponseEntity.ok(mapToDTO(part));
    }

    @GetMapping("/parts")
    public ResponseEntity<List<PartDTO>> getAllParts() {
        List<PartDTO> partsList = partRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(partsList);
    }

    @GetMapping("/usage-history")
    public ResponseEntity<List<PartUsage>> getUsageHistory() {
        return ResponseEntity.ok(partUsageRepository.findAll());
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    private PartDTO mapToDTO(Part part) {
        PartDTO dto = new PartDTO();
        dto.setPartId(part.getPartId());
        dto.setPartName(part.getPartName());
        dto.setCurrentStock(part.getCurrentStock());
        dto.setCategoryId(part.getCategoryId());
        dto.setMachineId(part.getMachineId());
        dto.setMinStock(part.getMinStock());
        dto.setManufactureDate(part.getManufactureDate());
        dto.setExpiryDate(part.getExpiryDate());
        dto.setWarrantyExpiryDate(part.getWarrantyExpiryDate());
        dto.setShelfLifeDays(part.getShelfLifeDays());
        dto.setConditionStatus(part.getConditionStatus());
        dto.setLifecycleStatus(part.getLifecycleStatus());
        return dto;
    }
}