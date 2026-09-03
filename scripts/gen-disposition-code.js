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

// ===================== DISPOSITION =====================
write('disposition/entity/Disposition.java', `package ${PKG}.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "DISPOSITION")
@Getter
@Setter
public class Disposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DISPOSITION_ID")
    private Long id;

    @Column(name = "RECEPTION_ID", length = 20)
    private String receptionId;

    @Column(name = "DISPOSITION_TYPE_CODE", length = 20)
    private String dispositionTypeCode;

    @Column(name = "DECIDED_BY_ID", length = 36)
    private String decidedById;

    @Column(name = "DECIDED_AT")
    private LocalDateTime decidedAt;
${AUDIT}
}
`);

write('disposition/entity/AdmissionRequest.java', `package ${PKG}.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ADMISSION_REQUEST")
@Getter
@Setter
public class AdmissionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADMISSION_REQUEST_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "TARGET_DEPT_CODE", length = 20)
    private String targetDeptCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;
${AUDIT}
}
`);

write('disposition/entity/TransferNote.java', `package ${PKG}.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "TRANSFER_NOTE")
@Getter
@Setter
public class TransferNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSFER_NOTE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "TARGET_HOSPITAL_CODE", length = 20)
    private String targetHospitalCode;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "WRITTEN_BY_ID", length = 36)
    private String writtenById;

    @Column(name = "WRITTEN_AT")
    private LocalDateTime writtenAt;
${AUDIT}
}
`);

write('disposition/entity/AmbulanceTransport.java', `package ${PKG}.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "AMBULANCE_TRANSPORT")
@Getter
@Setter
public class AmbulanceTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AMBULANCE_TRANSPORT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "AMBULANCE_NO", length = 20)
    private String ambulanceNo;

    @Column(name = "TRANSPORT_TYPE_CODE", length = 20)
    private String transportTypeCode;

    @Column(name = "DEPARTED_AT")
    private LocalDateTime departedAt;

    @Column(name = "ARRIVED_AT")
    private LocalDateTime arrivedAt;

    @Column(name = "ACCOMPANYING_STAFF_ID", length = 36)
    private String accompanyingStaffId;
${AUDIT}
}
`);

write('disposition/repository/DispositionRepository.java', `package ${PKG}.disposition.repository;

import ${PKG}.disposition.entity.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositionRepository extends JpaRepository<Disposition, Long> {
}
`);

write('disposition/repository/AdmissionRequestRepository.java', `package ${PKG}.disposition.repository;

import ${PKG}.disposition.entity.AdmissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionRequestRepository extends JpaRepository<AdmissionRequest, Long> {
}
`);

write('disposition/repository/TransferNoteRepository.java', `package ${PKG}.disposition.repository;

import ${PKG}.disposition.entity.TransferNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferNoteRepository extends JpaRepository<TransferNote, Long> {
}
`);

write('disposition/repository/AmbulanceTransportRepository.java', `package ${PKG}.disposition.repository;

import ${PKG}.disposition.entity.AmbulanceTransport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceTransportRepository extends JpaRepository<AmbulanceTransport, Long> {
}
`);

write('disposition/dto/DispositionCreateRequestDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositionCreateRequestDto {
    private String encounterId;
    private String dispositionType;
    private String decidedById;
}
`);

write('disposition/dto/DispositionDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DispositionDto {
    private Long id;
    private String receptionId;
    private String dispositionTypeCode;
    private String decidedById;
    private LocalDateTime decidedAt;
}
`);

write('disposition/dto/AdmissionRequestCreateDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionRequestCreateDto {
    private String wardPrefer;
    private String targetDeptCode;
}
`);

write('disposition/dto/AdmissionRequestDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdmissionRequestDto {
    private Long id;
    private Long dispositionId;
    private String targetDeptCode;
    private String requestStatusCode;
    private LocalDateTime requestedAt;
}
`);

write('disposition/dto/TransferNoteCreateDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferNoteCreateDto {
    private String targetHospitalCode;
    private String content;
    private String writtenById;
}
`);

write('disposition/dto/TransferNoteDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransferNoteDto {
    private Long id;
    private Long dispositionId;
    private String targetHospitalCode;
    private String content;
    private String writtenById;
    private LocalDateTime writtenAt;
}
`);

write('disposition/dto/AmbulanceTransportCreateDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AmbulanceTransportCreateDto {
    private String ambulanceNo;
    private String transportTypeCode;
    private LocalDateTime departedAt;
    private LocalDateTime arrivedAt;
    private String accompanyingStaffId;
}
`);

write('disposition/dto/AmbulanceTransportDto.java', `package ${PKG}.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AmbulanceTransportDto {
    private Long id;
    private Long dispositionId;
    private String ambulanceNo;
    private String transportTypeCode;
    private LocalDateTime departedAt;
    private LocalDateTime arrivedAt;
    private String accompanyingStaffId;
}
`);

write('disposition/service/DispositionService.java', `package ${PKG}.disposition.service;

import ${PKG}.disposition.dto.*;

public interface DispositionService {
    DispositionDto createDisposition(DispositionCreateRequestDto request);
    AdmissionRequestDto createAdmissionRequest(Long dispositionId, AdmissionRequestCreateDto request);
    TransferNoteDto createTransferNote(Long dispositionId, TransferNoteCreateDto request);
    AmbulanceTransportDto createAmbulanceTransport(Long dispositionId, AmbulanceTransportCreateDto request);
}
`);

write('disposition/service/DispositionServiceImpl.java', `package ${PKG}.disposition.service;

import ${PKG}.disposition.dto.*;
import ${PKG}.disposition.entity.*;
import ${PKG}.disposition.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DispositionServiceImpl implements DispositionService {

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final AmbulanceTransportRepository ambulanceTransportRepository;

    @Override
    @Transactional
    public DispositionDto createDisposition(DispositionCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getDispositionType())) {
            throw new IllegalArgumentException("encounterId and dispositionType are required");
        }
        Disposition entity = new Disposition();
        entity.setReceptionId(request.getEncounterId());
        entity.setDispositionTypeCode(request.getDispositionType());
        entity.setDecidedById(request.getDecidedById());
        entity.setDecidedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return toDispositionDto(dispositionRepository.save(entity));
    }

    @Override
    @Transactional
    public AdmissionRequestDto createAdmissionRequest(Long dispositionId, AdmissionRequestCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        AdmissionRequest entity = new AdmissionRequest();
        entity.setDisposition(disposition);
        entity.setTargetDeptCode(StringUtils.hasText(request.getTargetDeptCode())
                ? request.getTargetDeptCode() : request.getWardPrefer());
        entity.setRequestStatusCode("REQUESTED");
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AdmissionRequest saved = admissionRequestRepository.save(entity);

        AdmissionRequestDto dto = new AdmissionRequestDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setTargetDeptCode(saved.getTargetDeptCode());
        dto.setRequestStatusCode(saved.getRequestStatusCode());
        dto.setRequestedAt(saved.getRequestedAt());
        return dto;
    }

    @Override
    @Transactional
    public TransferNoteDto createTransferNote(Long dispositionId, TransferNoteCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        TransferNote entity = new TransferNote();
        entity.setDisposition(disposition);
        entity.setTargetHospitalCode(request.getTargetHospitalCode());
        entity.setContent(request.getContent());
        entity.setWrittenById(request.getWrittenById());
        entity.setWrittenAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        TransferNote saved = transferNoteRepository.save(entity);

        TransferNoteDto dto = new TransferNoteDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setTargetHospitalCode(saved.getTargetHospitalCode());
        dto.setContent(saved.getContent());
        dto.setWrittenById(saved.getWrittenById());
        dto.setWrittenAt(saved.getWrittenAt());
        return dto;
    }

    @Override
    @Transactional
    public AmbulanceTransportDto createAmbulanceTransport(Long dispositionId, AmbulanceTransportCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        AmbulanceTransport entity = new AmbulanceTransport();
        entity.setDisposition(disposition);
        entity.setAmbulanceNo(request.getAmbulanceNo());
        entity.setTransportTypeCode(request.getTransportTypeCode());
        entity.setDepartedAt(request.getDepartedAt());
        entity.setArrivedAt(request.getArrivedAt());
        entity.setAccompanyingStaffId(request.getAccompanyingStaffId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AmbulanceTransport saved = ambulanceTransportRepository.save(entity);

        AmbulanceTransportDto dto = new AmbulanceTransportDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setAmbulanceNo(saved.getAmbulanceNo());
        dto.setTransportTypeCode(saved.getTransportTypeCode());
        dto.setDepartedAt(saved.getDepartedAt());
        dto.setArrivedAt(saved.getArrivedAt());
        dto.setAccompanyingStaffId(saved.getAccompanyingStaffId());
        return dto;
    }

    private Disposition findDisposition(Long id) {
        return dispositionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("disposition not found: " + id));
    }

    private DispositionDto toDispositionDto(Disposition entity) {
        DispositionDto dto = new DispositionDto();
        dto.setId(entity.getId());
        dto.setReceptionId(entity.getReceptionId());
        dto.setDispositionTypeCode(entity.getDispositionTypeCode());
        dto.setDecidedById(entity.getDecidedById());
        dto.setDecidedAt(entity.getDecidedAt());
        return dto;
    }
}
`);

write('disposition/controller/DispositionController.java', `package ${PKG}.disposition.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.disposition.dto.*;
import ${PKG}.disposition.service.DispositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/dispositions")
@RequiredArgsConstructor
public class DispositionController {

    private final DispositionService dispositionService;

    @PostMapping
    public ApiResponse<DispositionDto> createDisposition(@RequestBody DispositionCreateRequestDto request) {
        return ApiResponse.success(dispositionService.createDisposition(request));
    }

    @PostMapping("/{id}/admission-request")
    public ApiResponse<AdmissionRequestDto> createAdmissionRequest(
            @PathVariable Long id,
            @RequestBody AdmissionRequestCreateDto request) {
        return ApiResponse.success(dispositionService.createAdmissionRequest(id, request));
    }

    @PostMapping("/{id}/transfer-note")
    public ApiResponse<TransferNoteDto> createTransferNote(
            @PathVariable Long id,
            @RequestBody TransferNoteCreateDto request) {
        return ApiResponse.success(dispositionService.createTransferNote(id, request));
    }

    @PostMapping("/{id}/ambulance-transport")
    public ApiResponse<AmbulanceTransportDto> createAmbulanceTransport(
            @PathVariable Long id,
            @RequestBody AmbulanceTransportCreateDto request) {
        return ApiResponse.success(dispositionService.createAmbulanceTransport(id, request));
    }
}
`);

// ===================== CODE (EMG) =====================
write('code/entity/EmgCodeGroup.java', `package ${PKG}.code.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EMG_CODE_GROUP")
@Getter
@Setter
public class EmgCodeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMG_CODE_GROUP_ID")
    private Long id;

    @Column(name = "GROUP_CODE", length = 30, unique = true, nullable = false)
    private String groupCode;

    @Column(name = "GROUP_NAME", length = 100)
    private String groupName;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;
${AUDIT}
}
`);

write('code/entity/EmgCode.java', `package ${PKG}.code.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EMG_CODE")
@Getter
@Setter
public class EmgCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMG_CODE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMG_CODE_GROUP_ID", nullable = false)
    private EmgCodeGroup codeGroup;

    @Column(name = "CODE_VALUE", length = 30, nullable = false)
    private String codeValue;

    @Column(name = "CODE_NAME", length = 100)
    private String codeName;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "USE_YN", length = 1)
    private String useYn;
${AUDIT}
}
`);

write('code/repository/EmgCodeGroupRepository.java', `package ${PKG}.code.repository;

import ${PKG}.code.entity.EmgCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmgCodeGroupRepository extends JpaRepository<EmgCodeGroup, Long> {
    Optional<EmgCodeGroup> findByGroupCode(String groupCode);
}
`);

write('code/repository/EmgCodeRepository.java', `package ${PKG}.code.repository;

import ${PKG}.code.entity.EmgCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmgCodeRepository extends JpaRepository<EmgCode, Long> {
    List<EmgCode> findByCodeGroup_GroupCodeOrderBySortOrderAsc(String groupCode);
}
`);

write('code/dto/EmgCodeGroupDto.java', `package ${PKG}.code.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class EmgCodeGroupDto {
    private Long id;
    private String groupCode;
    private String groupName;
    private String description;
    private String useYn;
    private Integer sortOrder;
    private List<EmgCodeDto> codes;
}
`);

write('code/dto/EmgCodeDto.java', `package ${PKG}.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeDto {
    private Long id;
    private String groupCode;
    private String codeValue;
    private String codeName;
    private String description;
    private Integer sortOrder;
    private String useYn;
}
`);

write('code/dto/EmgCodeCreateRequestDto.java', `package ${PKG}.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeCreateRequestDto {
    private String groupCode;
    private String groupName;
    private String groupDescription;
    private String codeValue;
    private String codeName;
    private String description;
    private Integer sortOrder;
}
`);

write('code/dto/EmgCodeUpdateRequestDto.java', `package ${PKG}.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeUpdateRequestDto {
    private String codeName;
    private String description;
    private Integer sortOrder;
}
`);

write('code/dto/EmgCodeUseYnRequestDto.java', `package ${PKG}.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeUseYnRequestDto {
    private String useYn;
}
`);

write('code/mapper/EmgCodeMapstructMapper.java', `package ${PKG}.code.mapper;

import ${PKG}.code.dto.EmgCodeDto;
import ${PKG}.code.dto.EmgCodeGroupDto;
import ${PKG}.code.entity.EmgCode;
import ${PKG}.code.entity.EmgCodeGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EmgCodeMapstructMapper {

    EmgCodeGroupDto toGroupDto(EmgCodeGroup group);

    @Mapping(target = "groupCode", source = "codeGroup.groupCode")
    EmgCodeDto toCodeDto(EmgCode code);

    List<EmgCodeDto> toCodeDtoList(List<EmgCode> codes);
}
`);

write('code/service/EmgCodeService.java', `package ${PKG}.code.service;

import ${PKG}.code.dto.*;
import java.util.List;

public interface EmgCodeService {
    List<EmgCodeGroupDto> getCodes();
    List<EmgCodeDto> getCodesByGroup(String groupCode);
    EmgCodeDto createCode(EmgCodeCreateRequestDto request);
    EmgCodeDto updateCode(Long codeId, EmgCodeUpdateRequestDto request);
    EmgCodeDto updateUseYn(Long codeId, EmgCodeUseYnRequestDto request);
}
`);

write('code/service/EmgCodeServiceImpl.java', `package ${PKG}.code.service;

import ${PKG}.code.dto.*;
import ${PKG}.code.entity.EmgCode;
import ${PKG}.code.entity.EmgCodeGroup;
import ${PKG}.code.mapper.EmgCodeMapstructMapper;
import ${PKG}.code.repository.EmgCodeGroupRepository;
import ${PKG}.code.repository.EmgCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmgCodeServiceImpl implements EmgCodeService {

    private final EmgCodeGroupRepository codeGroupRepository;
    private final EmgCodeRepository codeRepository;
    private final EmgCodeMapstructMapper codeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmgCodeGroupDto> getCodes() {
        return codeGroupRepository.findAll().stream().map(group -> {
            EmgCodeGroupDto dto = codeMapper.toGroupDto(group);
            dto.setCodes(codeMapper.toCodeDtoList(
                    codeRepository.findByCodeGroup_GroupCodeOrderBySortOrderAsc(group.getGroupCode())));
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmgCodeDto> getCodesByGroup(String groupCode) {
        return codeMapper.toCodeDtoList(
                codeRepository.findByCodeGroup_GroupCodeOrderBySortOrderAsc(groupCode));
    }

    @Override
    @Transactional
    public EmgCodeDto createCode(EmgCodeCreateRequestDto request) {
        if (!StringUtils.hasText(request.getGroupCode()) || !StringUtils.hasText(request.getCodeValue())) {
            throw new IllegalArgumentException("groupCode and codeValue are required");
        }
        EmgCodeGroup group = codeGroupRepository.findByGroupCode(request.getGroupCode())
                .orElseGet(() -> {
                    EmgCodeGroup g = new EmgCodeGroup();
                    g.setGroupCode(request.getGroupCode());
                    g.setGroupName(request.getGroupName());
                    g.setDescription(request.getGroupDescription());
                    g.setUseYn("Y");
                    g.setSortOrder(0);
                    g.setCreatedAt(LocalDateTime.now());
                    g.setUpdatedAt(LocalDateTime.now());
                    return codeGroupRepository.save(g);
                });

        EmgCode code = new EmgCode();
        code.setCodeGroup(group);
        code.setCodeValue(request.getCodeValue());
        code.setCodeName(request.getCodeName());
        code.setDescription(request.getDescription());
        code.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        code.setUseYn("Y");
        code.setCreatedAt(LocalDateTime.now());
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(codeRepository.save(code));
    }

    @Override
    @Transactional
    public EmgCodeDto updateCode(Long codeId, EmgCodeUpdateRequestDto request) {
        EmgCode code = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("code not found: " + codeId));
        if (request.getCodeName() != null) {
            code.setCodeName(request.getCodeName());
        }
        if (request.getDescription() != null) {
            code.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            code.setSortOrder(request.getSortOrder());
        }
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(code);
    }

    @Override
    @Transactional
    public EmgCodeDto updateUseYn(Long codeId, EmgCodeUseYnRequestDto request) {
        if (!"Y".equals(request.getUseYn()) && !"N".equals(request.getUseYn())) {
            throw new IllegalArgumentException("useYn must be Y or N");
        }
        EmgCode code = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("code not found: " + codeId));
        code.setUseYn(request.getUseYn());
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(code);
    }
}
`);

write('code/controller/EmgCodeController.java', `package ${PKG}.code.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.code.dto.*;
import ${PKG}.code.service.EmgCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/codes")
@RequiredArgsConstructor
public class EmgCodeController {

    private final EmgCodeService emgCodeService;

    @GetMapping
    public ApiResponse<List<EmgCodeGroupDto>> getCodes() {
        return ApiResponse.success(emgCodeService.getCodes());
    }

    @GetMapping("/{groupCode}")
    public ApiResponse<List<EmgCodeDto>> getCodesByGroup(@PathVariable String groupCode) {
        return ApiResponse.success(emgCodeService.getCodesByGroup(groupCode));
    }

    @PostMapping
    public ApiResponse<EmgCodeDto> createCode(@RequestBody EmgCodeCreateRequestDto request) {
        return ApiResponse.success(emgCodeService.createCode(request));
    }

    @PutMapping("/{codeId}")
    public ApiResponse<EmgCodeDto> updateCode(
            @PathVariable Long codeId,
            @RequestBody EmgCodeUpdateRequestDto request) {
        return ApiResponse.success(emgCodeService.updateCode(codeId, request));
    }

    @PutMapping("/{codeId}/use-yn")
    public ApiResponse<EmgCodeDto> updateUseYn(
            @PathVariable Long codeId,
            @RequestBody EmgCodeUseYnRequestDto request) {
        return ApiResponse.success(emgCodeService.updateUseYn(codeId, request));
    }
}
`);

console.log('disposition+code done');
