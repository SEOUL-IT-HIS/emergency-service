package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.disposition.messaging.AdmissionEventPublisher;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
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

    // 코드그룹 DISPOSITION_TYPE_CD (EmgCodes). admin 캐시에 없으면 폴백 사용

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final CommonCodeCache commonCodeCache;
    private final CommonCodeResolver codeResolver;
    private final AdmissionEventPublisher admissionEventPublisher;

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
        if (!EmgCodes.DISPOSITION_ADMIT.equals(disposition.getDispositionTypeCode())) {
            throw new IllegalArgumentException("admission request requires a disposition of type ADMIT");
        }
        requireIfKnown("targetDeptCode", request.getTargetDeptCode(), EmgCodes.DEPT_GROUP);
        requireIfKnown("wardPrefer", request.getWardPrefer(), EmgCodes.WARD_GROUP);
        boolean alreadyActive = admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc(dispositionId).stream()
                .anyMatch(a -> !EmgCodes.ADMISSION_REJECTED.equals(a.getRequestStatusCode()));
        if (alreadyActive) {
            // 거부된 뒤에는 다시 요청할 수 있지만, 요청됨/병상 배정 완료 상태의 요청이 있으면 중복 요청을 막는다
            throw new ConflictException("admission request already exists for disposition: " + dispositionId);
        }
        AdmissionRequest entity = new AdmissionRequest();
        entity.setDisposition(disposition);
        entity.setTargetDeptCode(StringUtils.hasText(request.getTargetDeptCode())
                ? request.getTargetDeptCode() : request.getWardPrefer());
        entity.setRequestStatusCode(EmgCodes.ADMISSION_REQUESTED);
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AdmissionRequest saved = admissionRequestRepository.save(entity);
        // 병동으로 입원요청 이벤트 발행(설정이 꺼져 있으면 로그만). 발행 실패가 저장을 막지 않는다.
        admissionEventPublisher.publishRequested(disposition, saved, request.getWardPrefer());

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
        if (!EmgCodes.DISPOSITION_TRANSFER.equals(disposition.getDispositionTypeCode())) {
            throw new IllegalArgumentException("transfer note requires a disposition of type TRANSFER");
        }
        if (!StringUtils.hasText(request.getTargetHospitalCode()) || !StringUtils.hasText(request.getContent())
                || !StringUtils.hasText(request.getWrittenById())) {
            throw new IllegalArgumentException("targetHospitalCode, content, writtenById are required");
        }
        codeResolver.require("targetHospitalCode", request.getTargetHospitalCode(),
                codeResolver.valueSet(EmgCodes.TRANSFER_HOSPITAL_GROUP, EmgCodes.TRANSFER_HOSPITAL_FALLBACK));
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
        List<AdminCommonCodeItemDto> codes = commonCodeCache.get(EmgCodes.DISPOSITION_TYPE_GROUP);
        if (codes.isEmpty()) {
            return new java.util.LinkedHashSet<>(EmgCodes.DISPOSITION_TYPE_FALLBACK);
        }
        return codes.stream()
                .filter(code -> !"N".equals(code.getUseYn()))
                .map(AdminCommonCodeItemDto::getCodeValue)
                .collect(Collectors.toSet());
    }

    /** admin 그룹이 캐시에 있을 때만 값을 검증한다(admin 이 꺼져 있으면 값 검증 없이 통과). 값이 비어 있으면 건너뛴다. */
    private void requireIfKnown(String field, String value, String groupCode) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Set<String> known = codeResolver.valueSet(groupCode, java.util.List.of());
        if (!known.isEmpty()) {
            codeResolver.require(field, value, known);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdmissionRequestDto> getAdmissionRequests(String dispositionId) {
        requireId(dispositionId);
        return admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc(dispositionId).stream()
                .map(this::toAdmissionDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdmissionRequestDto updateAdmissionStatus(String dispositionId, String statusCode) {
        requireId(dispositionId);
        codeResolver.require("requestStatusCode", statusCode,
                codeResolver.valueSet(EmgCodes.ADMISSION_STATUS_GROUP, EmgCodes.ADMISSION_STATUS_FALLBACK));
        // 가장 최근 요청(진행 중인 것)에 병동 회신을 반영한다
        AdmissionRequest latest = admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc(dispositionId).stream()
                .findFirst().orElseThrow(() -> ResourceNotFoundException.of("admissionRequest", dispositionId));
        latest.setRequestStatusCode(statusCode);
        latest.setUpdatedAt(LocalDateTime.now());
        return toAdmissionDto(latest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferNoteDto> getTransferNotes(String dispositionId) {
        requireId(dispositionId);
        return transferNoteRepository.findByDispositionIdOrderByWrittenAtDesc(dispositionId).stream()
                .map(this::toTransferDto).collect(Collectors.toList());
    }

    private void requireId(String dispositionId) {
        if (!StringUtils.hasText(dispositionId)) {
            throw new IllegalArgumentException("dispositionId is required");
        }
    }

    private AdmissionRequestDto toAdmissionDto(AdmissionRequest saved) {
        AdmissionRequestDto dto = new AdmissionRequestDto();
        dto.setId(saved.getId());
        dto.setDispositionId(saved.getDisposition().getId());
        dto.setTargetDeptCode(saved.getTargetDeptCode());
        dto.setRequestStatusCode(saved.getRequestStatusCode());
        dto.setRequestedAt(saved.getRequestedAt());
        return dto;
    }

    private TransferNoteDto toTransferDto(TransferNote saved) {
        TransferNoteDto dto = new TransferNoteDto();
        dto.setId(saved.getId());
        dto.setDispositionId(saved.getDisposition().getId());
        dto.setTargetHospitalCode(saved.getTargetHospitalCode());
        dto.setContent(saved.getContent());
        dto.setWrittenById(saved.getWrittenById());
        dto.setWrittenAt(saved.getWrittenAt());
        return dto;
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
