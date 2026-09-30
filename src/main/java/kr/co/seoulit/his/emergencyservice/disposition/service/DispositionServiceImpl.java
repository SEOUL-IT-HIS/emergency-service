package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.*;
import kr.co.seoulit.his.emergencyservice.disposition.entity.*;
import kr.co.seoulit.his.emergencyservice.disposition.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispositionServiceImpl implements DispositionService {

    // 코드그룹 DISPOSITION_TYPE (docs/models.md). admin 캐시에 없으면 폴백 사용
    private static final String DISPOSITION_TYPE_GROUP_CODE = "DISPOSITION_TYPE";
    private static final Set<String> DISPOSITION_TYPE_FALLBACK = Set.of("HOME", "ADMIT", "TRANSFER", "DEATH", "DAMA");

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final CommonCodeCache commonCodeCache;

    @Override
    @Transactional
    public DispositionDto createDisposition(DispositionCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getDispositionType())) {
            throw new IllegalArgumentException("encounterId and dispositionType are required");
        }
        Set<String> validTypes = validDispositionTypes();
        if (!validTypes.contains(request.getDispositionType())) {
            throw new IllegalArgumentException("dispositionType must be one of " + String.join(", ", validTypes));
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
    public AdmissionRequestDto createAdmissionRequest(String dispositionId, AdmissionRequestCreateDto request) {
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
    public TransferNoteDto createTransferNote(String dispositionId, TransferNoteCreateDto request) {
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
    @Transactional(readOnly = true)
    public List<DispositionDto> getDispositions(String receptionId) {
        if (!StringUtils.hasText(receptionId)) {
            throw new IllegalArgumentException("receptionId is required");
        }
        return dispositionRepository.findByReceptionIdOrderByDecidedAtDesc(receptionId).stream()
                .map(this::toDispositionDto)
                .collect(Collectors.toList());
    }

    private Set<String> validDispositionTypes() {
        List<AdminCommonCodeItemDto> codes = commonCodeCache.get(DISPOSITION_TYPE_GROUP_CODE);
        if (codes.isEmpty()) {
            return DISPOSITION_TYPE_FALLBACK;
        }
        return codes.stream()
                .filter(code -> !"N".equals(code.getUseYn()))
                .map(AdminCommonCodeItemDto::getCodeValue)
                .collect(Collectors.toSet());
    }

    private Disposition findDisposition(String id) {
        return dispositionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("disposition", id));
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
