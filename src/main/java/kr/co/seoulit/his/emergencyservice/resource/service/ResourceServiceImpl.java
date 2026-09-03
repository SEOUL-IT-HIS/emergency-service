package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.resource.dto.*;
import kr.co.seoulit.his.emergencyservice.resource.entity.*;
import kr.co.seoulit.his.emergencyservice.resource.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentAllocationRepository equipmentAllocationRepository;

    @Override
    @Transactional(readOnly = true)
    public CongestionDto getCongestion() {
        long total = bedRepository.count();
        long occupied = bedRepository.countByBedStatusCode("OCCUPIED");
        long empty = bedRepository.countByBedStatusCode("EMPTY");
        CongestionDto dto = new CongestionDto();
        dto.setTotalBeds(total);
        dto.setOccupiedBeds(occupied);
        dto.setEmptyBeds(empty);
        dto.setOccupancyRate(total == 0 ? 0 : (occupied * 100.0) / total);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedDto> getBeds(String zoneCode, String status) {
        return bedRepository.findAll().stream()
                .filter(bed -> !StringUtils.hasText(zoneCode) || zoneCode.equals(bed.getZoneCode()))
                .filter(bed -> !StringUtils.hasText(status) || status.equals(bed.getBedStatusCode()))
                .map(bed -> {
                    BedDto dto = new BedDto();
                    dto.setId(bed.getId());
                    dto.setBedNo(bed.getBedNo());
                    dto.setZoneCode(bed.getZoneCode());
                    dto.setBedTypeCode(bed.getBedTypeCode());
                    dto.setBedStatusCode(bed.getBedStatusCode());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getBedId() == null) {
            throw new IllegalArgumentException("encounterId and bedId are required");
        }
        Bed bed = bedRepository.findById(request.getBedId())
                .orElseThrow(() -> ResourceNotFoundException.of("bed", request.getBedId()));
        if ("OCCUPIED".equals(bed.getBedStatusCode())) {
            throw new ConflictException("bed already occupied: " + request.getBedId());
        }
        BedAssignment assignment = new BedAssignment();
        assignment.setReceptionId(request.getEncounterId());
        assignment.setBed(bed);
        assignment.setAssignedById(request.getAssignedById());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        BedAssignment saved = bedAssignmentRepository.save(assignment);

        bed.setBedStatusCode("OCCUPIED");
        bed.setUpdatedAt(LocalDateTime.now());

        BedAssignmentDto dto = new BedAssignmentDto();
        dto.setId(saved.getId());
        dto.setReceptionId(saved.getReceptionId());
        dto.setBedId(bed.getId());
        dto.setBedNo(bed.getBedNo());
        dto.setZoneCode(bed.getZoneCode());
        dto.setAssignedById(saved.getAssignedById());
        dto.setAssignedAt(saved.getAssignedAt());
        return dto;
    }

    @Override
    @Transactional
    public EquipmentAllocationDto assignEquipment(EquipmentAssignmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getEquipmentId() == null) {
            throw new IllegalArgumentException("encounterId and equipmentId are required");
        }
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("equipment", request.getEquipmentId()));
        if ("IN_USE".equals(equipment.getEquipmentStatusCode())) {
            throw new ConflictException("equipment already in use: " + request.getEquipmentId());
        }
        EquipmentAllocation allocation = new EquipmentAllocation();
        allocation.setReceptionId(request.getEncounterId());
        allocation.setEquipment(equipment);
        allocation.setAllocatedById(request.getAllocatedById());
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setCreatedAt(LocalDateTime.now());
        allocation.setUpdatedAt(LocalDateTime.now());
        EquipmentAllocation saved = equipmentAllocationRepository.save(allocation);

        equipment.setEquipmentStatusCode("IN_USE");
        equipment.setUpdatedAt(LocalDateTime.now());

        EquipmentAllocationDto dto = new EquipmentAllocationDto();
        dto.setId(saved.getId());
        dto.setReceptionId(saved.getReceptionId());
        dto.setEquipmentId(equipment.getId());
        dto.setAssetNo(equipment.getAssetNo());
        dto.setAllocatedById(saved.getAllocatedById());
        dto.setAllocatedAt(saved.getAllocatedAt());
        return dto;
    }
}
