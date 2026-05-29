//package com.tcs.machcare.bo;
//import com.tcs.machcare.entity.Part;
//
//import com.tcs.machcare.entity.PartUsage;
//import com.tcs.machcare.repository.PartRepository;
//import com.tcs.machcare.repository.PartUsageRepository;
//import com.tcs.machcare.exception.InventoryException;
//
//import org.springframework.stereotype.Service;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//public class InventoryBO {
//
//    @Autowired
//    private PartRepository partRepository;
//
//    @Autowired
//    private PartUsageRepository partUsageRepository;
//
//    /**
//     * ✅ Use Part
//     */
//    @Transactional
//    public void usePart(Long partId, Long empId, int quantity) {
//
//        Part part = partRepository.findById(partId)
//                .orElseThrow(() -> new InventoryException("Part not found"));
//
//        if (part.getCurrentStock() < quantity) {
//            throw new InventoryException("Insufficient stock");
//        }
//
//        // ✅ reduce stock
//        part.setCurrentStock(part.getCurrentStock() - quantity);
//        partRepository.save(part);
//
//        // ✅ record usage
//        PartUsage usage = new PartUsage();
//        usage.setPartId(partId);
//        usage.setEmpId(empId);
//        usage.setQtyAssigned(quantity);
//        usage.setLastUpdated(LocalDateTime.now());
//
//        partUsageRepository.save(usage);
//    }
//
//    /**
//     * ✅ Get Part
//     */
//    public Part getPartById(Long id) {
//        return partRepository.findById(id)
//                .orElseThrow(() -> new InventoryException("Part not found"));
//    }
//
//    /**
//     * ✅ Get All
//     */
//    public List<Part> getAllParts() {
//        return partRepository.findAll();
//    }
//
//    /**
//     * ✅ Create
//     */
//    public Long createPart(Part part) {
//        return partRepository.save(part).getPartId();
//    }
//
//    /**
//     * ✅ Correct Stock
//     */
//    public void correctStock(Long partId, int newStock) {
//        Part part = getPartById(partId);
//        part.setCurrentStock(newStock);
//        partRepository.save(part);
//    }
//
//    /**
//     * ✅ Usage History
//     */
//    public List<PartUsage> getUsageHistory() {
//        return partUsageRepository.findAll();
//    }
//}
package com.tcs.Machcare.service;

import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.PartUsage;
import com.tcs.Machcare.repository.PartRepository;
import com.tcs.Machcare.repository.PartUsageRepository;
import com.tcs.Machcare.exception.InventoryException;
import com.tcs.Machcare.dto.*;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private PartUsageRepository partUsageRepository;

    @Autowired
    private AssetLifecycleService assetLifecycleService;

    /**
     * ✅ Use Part (DTO based)
     */
    @Transactional
    public void usePart(UsePartRequestDTO dto) {

        Part part = partRepository.findById(dto.getPartId())
                .orElseThrow(() -> new InventoryException("Part not found"));

        if (part.getCurrentStock() < dto.getQuantity()) {
            throw new InventoryException("Insufficient stock");
        }

        // update stock
        part.setCurrentStock(part.getCurrentStock() - dto.getQuantity());
        assetLifecycleService.refreshPartLifecycle(partRepository.save(part));

        // record usage
        PartUsage usage = new PartUsage();
        usage.setPartId(dto.getPartId());
        usage.setEmpId(dto.getEmpId());
        usage.setQtyAssigned(dto.getQuantity());
        usage.setLastUpdated(LocalDateTime.now());

        partUsageRepository.save(usage);
    }

    /**
     * ✅ Get Part (DTO response)
     */
    public PartDTO getPartById(Long id) {

        Part part = partRepository.findById(id)
                .orElseThrow(() -> new InventoryException("Part not found"));

        return mapToDTO(part);
    }

    /**
     * ✅ Get all (DTO list)
     */
    public List<PartDTO> getAllParts() {
        return partRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Create Part (DTO input)
     */
    public Long createPart(CreatePartRequestDTO dto) {

        validatePartRequest(dto);

        Part part = new Part();
        part.setPartName(dto.getPartName());
        part.setCategoryId(1L);
        part.setMachineId(dto.getMachineId());
        part.setMinStock(dto.getMinStock());
        part.setCurrentStock(dto.getCurrentStock());
        part.setManufactureDate(dto.getManufactureDate());
        part.setExpiryDate(dto.getExpiryDate());
        part.setWarrantyExpiryDate(dto.getWarrantyExpiryDate());
        part.setShelfLifeDays(dto.getShelfLifeDays());
        part.setConditionStatus(dto.getConditionStatus());

        return assetLifecycleService.refreshPartLifecycle(partRepository.save(part)).getPartId();
    }

    /**
     * ✅ Correct Stock
     */
    public void correctStock(Long partId, StockUpdateDTO dto) {

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new InventoryException("Part not found"));

        part.setCurrentStock(dto.getNewStock());
        assetLifecycleService.refreshPartLifecycle(partRepository.save(part));
    }

    /**
     * ✅ Usage History (basic)
     */
    public List<PartUsage> getUsageHistory() {
        return partUsageRepository.findAll();
    }

    /**
     * ✅ Mapper
     */
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

    private void validatePartRequest(CreatePartRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Part details are required.");
        }
        if (dto.getManufactureDate() != null && dto.getExpiryDate() != null
                && dto.getExpiryDate().isBefore(dto.getManufactureDate())) {
            throw new IllegalArgumentException("expiryDate must be on or after manufactureDate.");
        }
        if (dto.getShelfLifeDays() != null && dto.getShelfLifeDays() < 0) {
            throw new IllegalArgumentException("shelfLifeDays must be 0 or greater.");
        }
    }
}