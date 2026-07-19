const fs = require('fs');
const path = require('path');
const BASE = 'C:/his/emergency-service/src/main/java/kr/co/seoulit/his/emergencyservice';
const PKG = 'kr.co.seoulit.his.emergencyservice';

function write(rel, content) {
  const full = path.join(BASE, rel);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content, 'utf8');
  console.log('W', rel);
}

const AUDIT = `
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
`;

// ===================== RESOURCE =====================
write('resource/entity/Bed.java', `package ${PKG}.resource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "BED")
@Getter
@Setter
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BED_ID")
    private Long id;

    @Column(name = "BED_NO", length = 20, unique = true)
    private String bedNo;

    @Column(name = "ZONE_CODE", length = 20)
    private String zoneCode;

    @Column(name = "BED_TYPE_CODE", length = 20)
    private String bedTypeCode;

    @Column(name = "BED_STATUS_CODE", length = 20)
    private String bedStatusCode;
${AUDIT}
}
`);

write('resource/entity/BedAssignment.java', `package ${PKG}.resource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "BED_ASSIGNMENT")
@Getter
@Setter
public class BedAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BED_ASSIGNMENT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BED_ID", nullable = false)
    private Bed bed;

    @Column(name = "ASSIGNED_BY_ID", length = 36)
    private String assignedById;

    @Column(name = "ASSIGNED_AT")
    private LocalDateTime assignedAt;

    @Column(name = "RELEASED_AT")
    private LocalDateTime releasedAt;
${AUDIT}
}
`);

write('resource/entity/Equipment.java', `package ${PKG}.resource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "EQUIPMENT")
@Getter
@Setter
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EQUIPMENT_ID")
    private Long id;

    @Column(name = "ASSET_NO", length = 20, unique = true)
    private String assetNo;

    @Column(name = "EQUIPMENT_TYPE_CODE", length = 20)
    private String equipmentTypeCode;

    @Column(name = "EQUIPMENT_STATUS_CODE", length = 20)
    private String equipmentStatusCode;
${AUDIT}
}
`);

write('resource/entity/EquipmentAllocation.java', `package ${PKG}.resource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "EQUIPMENT_ALLOCATION")
@Getter
@Setter
public class EquipmentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EQUIPMENT_ALLOCATION_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EQUIPMENT_ID", nullable = false)
    private Equipment equipment;

    @Column(name = "ALLOCATED_BY_ID", length = 36)
    private String allocatedById;

    @Column(name = "ALLOCATED_AT")
    private LocalDateTime allocatedAt;

    @Column(name = "RETURNED_AT")
    private LocalDateTime returnedAt;
${AUDIT}
}
`);

write('resource/repository/BedRepository.java', `package ${PKG}.resource.repository;

import ${PKG}.resource.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BedRepository extends JpaRepository<Bed, Long> {
    long countByBedStatusCode(String bedStatusCode);
    List<Bed> findByBedStatusCode(String bedStatusCode);
}
`);

write('resource/repository/BedAssignmentRepository.java', `package ${PKG}.resource.repository;

import ${PKG}.resource.entity.BedAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BedAssignmentRepository extends JpaRepository<BedAssignment, Long> {
    List<BedAssignment> findByReceptionNoAndReleasedAtIsNull(String receptionNo);
}
`);

write('resource/repository/EquipmentRepository.java', `package ${PKG}.resource.repository;

import ${PKG}.resource.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
`);

write('resource/repository/EquipmentAllocationRepository.java', `package ${PKG}.resource.repository;

import ${PKG}.resource.entity.EquipmentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentAllocationRepository extends JpaRepository<EquipmentAllocation, Long> {
}
`);

write('resource/dto/CongestionDto.java', `package ${PKG}.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CongestionDto {
    private long totalBeds;
    private long occupiedBeds;
    private long emptyBeds;
    private double occupancyRate;
}
`);

write('resource/dto/BedAssignmentCreateRequestDto.java', `package ${PKG}.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BedAssignmentCreateRequestDto {
    private String encounterId;
    private Long bedId;
    private String assignedById;
}
`);

write('resource/dto/BedAssignmentDto.java', `package ${PKG}.resource.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class BedAssignmentDto {
    private Long id;
    private String receptionNo;
    private Long bedId;
    private String bedNo;
    private String zoneCode;
    private String assignedById;
    private LocalDateTime assignedAt;
}
`);

write('resource/dto/EquipmentAssignmentCreateRequestDto.java', `package ${PKG}.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentAssignmentCreateRequestDto {
    private String encounterId;
    private Long equipmentId;
    private String allocatedById;
}
`);

write('resource/dto/EquipmentAllocationDto.java', `package ${PKG}.resource.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EquipmentAllocationDto {
    private Long id;
    private String receptionNo;
    private Long equipmentId;
    private String assetNo;
    private String allocatedById;
    private LocalDateTime allocatedAt;
}
`);

write('resource/service/ResourceService.java', `package ${PKG}.resource.service;

import ${PKG}.resource.dto.*;

public interface ResourceService {
    CongestionDto getCongestion();
    BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request);
    EquipmentAllocationDto assignEquipment(EquipmentAssignmentCreateRequestDto request);
}
`);

write('resource/service/ResourceServiceImpl.java', `package ${PKG}.resource.service;

import ${PKG}.resource.dto.*;
import ${PKG}.resource.entity.*;
import ${PKG}.resource.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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
    @Transactional
    public BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getBedId() == null) {
            throw new IllegalArgumentException("encounterId and bedId are required");
        }
        Bed bed = bedRepository.findById(request.getBedId())
                .orElseThrow(() -> new IllegalArgumentException("bed not found: " + request.getBedId()));
        BedAssignment assignment = new BedAssignment();
        assignment.setReceptionNo(request.getEncounterId());
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
        dto.setReceptionNo(saved.getReceptionNo());
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
                .orElseThrow(() -> new IllegalArgumentException("equipment not found: " + request.getEquipmentId()));
        EquipmentAllocation allocation = new EquipmentAllocation();
        allocation.setReceptionNo(request.getEncounterId());
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
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setEquipmentId(equipment.getId());
        dto.setAssetNo(equipment.getAssetNo());
        dto.setAllocatedById(saved.getAllocatedById());
        dto.setAllocatedAt(saved.getAllocatedAt());
        return dto;
    }
}
`);

write('resource/controller/ResourceController.java', `package ${PKG}.resource.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.resource.dto.*;
import ${PKG}.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/congestion")
    public ApiResponse<CongestionDto> getCongestion() {
        return ApiResponse.success(resourceService.getCongestion());
    }

    @PostMapping("/bed-assignments")
    public ApiResponse<BedAssignmentDto> assignBed(@RequestBody BedAssignmentCreateRequestDto request) {
        return ApiResponse.success(resourceService.assignBed(request));
    }

    @PostMapping("/equipment-assignments")
    public ApiResponse<EquipmentAllocationDto> assignEquipment(
            @RequestBody EquipmentAssignmentCreateRequestDto request) {
        return ApiResponse.success(resourceService.assignEquipment(request));
    }
}
`);

console.log('resource done');
