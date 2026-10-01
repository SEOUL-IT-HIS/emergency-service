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
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispositionServiceImpl implements DispositionService {

    // 코드그룹 ER_DISPOSITION_TYPE_CD (EmgCodes). admin 캐시에 없으면 폴백 사용

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final CommonCodeCache commonCodeCache;
    private final CommonCodeResolver codeResolver;
    private final AdmissionEventPublisher admissionEventPublisher;
    private final DischargeProgress dischargeProgress;
    private final ResourceService resourceService;

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
        // 이미 결정이 있으면 '결정 변경'이다. 후속 조치 전(OPEN: 입원요청 없음·거부됨, 전원 소견서 없음)일 때만 허용한다.
        // 퇴실 처리가 끝났거나(DONE) 병동 회신을 기다리는 중(WAITING_WARD)이면 바꿀 수 없다. 이력은 새 행으로 쌓이고 최신 결정이 적용된다.
        DischargeProgress.Stage stage = dischargeProgress.stage(request.getEncounterId());
        if (stage == DischargeProgress.Stage.DONE || stage == DischargeProgress.Stage.WAITING_WARD) {
            throw new ConflictException("disposition cannot be changed (" + stage + ") for reception: "
                    + request.getEncounterId());
        }
        if (stage == DischargeProgress.Stage.OPEN) {
            String currentType = latestDisposition(request.getEncounterId()).map(Disposition::getDispositionTypeCode).orElse(null);
            if (request.getDispositionType().equals(currentType)) {
                throw new IllegalArgumentException("dispositionType is already " + currentType);
            }
        }
        Disposition entity = new Disposition();
        entity.setReceptionId(request.getEncounterId());
        entity.setDispositionTypeCode(request.getDispositionType());
        entity.setDecidedById(request.getDecidedById());
        entity.setDecidedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        DispositionDto dto = toDispositionDto(dispositionRepository.save(entity));
        releaseBedsIfDischarged(entity.getReceptionId());
        // 새 결정은 후속 조치가 없으니 입원·전원이면 아직 바꿀 수 있다(귀가·사망·자의퇴원은 즉시 완료)
        dto.setChangeable(EmgCodes.DISPOSITION_ADMIT.equals(entity.getDispositionTypeCode())
                || EmgCodes.DISPOSITION_TRANSFER.equals(entity.getDispositionTypeCode()));
        return dto;
    }

    @Override
    @Transactional
    public AdmissionRequestDto createAdmissionRequest(String dispositionId, AdmissionRequestCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        requireLatest(disposition);
        if (!EmgCodes.DISPOSITION_ADMIT.equals(disposition.getDispositionTypeCode())) {
            throw new IllegalArgumentException("admission request requires a disposition of type ADMIT");
        }
        requireIfKnown("targetDeptCode", request.getTargetDeptCode(), EmgCodes.DEPT_GROUP);
        requireIfKnown("wardPrefer", request.getWardPrefer(), EmgCodes.WARD_GROUP);
        if (request.getNote() != null && request.getNote().length() > 500) {
            throw new IllegalArgumentException("note must be at most 500 characters");
        }
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
        // DB 커밋 뒤에 발행한다: 커밋 전에 보내면 병동의 빠른 회신이 아직 없는 요청을 찾다가 유실될 수 있다.
        runAfterCommit(() -> admissionEventPublisher.publishRequested(
                disposition, saved, request.getWardPrefer(), request.getNote()));

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
        requireLatest(disposition);
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
        releaseBedsIfDischarged(disposition.getReceptionId());

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
        List<DispositionDto> result = dispositionRepository.findByReceptionIdOrderByDecidedAtDesc(receptionId).stream()
                .map(this::toDispositionDto)
                .collect(Collectors.toList());
        // 바꿀 수 있는 건 최신 결정뿐(이전 결정은 이력)
        if (!result.isEmpty()) {
            result.get(0).setChangeable(dischargeProgress.stage(receptionId) == DischargeProgress.Stage.OPEN);
        }
        return result;
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

    /** 트랜잭션 안이면 커밋 성공 뒤에 실행(롤백되면 실행 안 함), 트랜잭션 밖이면 바로 실행한다 */
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
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
    public AdmissionRequestDto updateAdmissionStatus(String dispositionId, String admissionRequestId, String statusCode,
                                                     String wardCode) {
        requireId(dispositionId);
        codeResolver.require("requestStatusCode", statusCode,
                codeResolver.valueSet(EmgCodes.ADMISSION_STATUS_GROUP, EmgCodes.ADMISSION_STATUS_FALLBACK));
        // 회신이 가리키는 요청(admissionRequestId)에, 없으면 가장 최근 요청에 반영한다
        List<AdmissionRequest> requests = admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc(dispositionId);
        AdmissionRequest target = (admissionRequestId == null
                ? requests.stream().findFirst()
                : requests.stream().filter(a -> admissionRequestId.equals(a.getId())).findFirst())
                .orElseThrow(() -> ResourceNotFoundException.of("admissionRequest", dispositionId));
        // 처음 회신만 반영한다: 이미 배정/거부된 요청은 뒤늦은 회신(중복·재전송)으로 덮어쓰지 않는다
        if (!EmgCodes.ADMISSION_REQUESTED.equals(target.getRequestStatusCode())) {
            return toAdmissionDto(target);
        }
        target.setRequestStatusCode(statusCode);
        if (EmgCodes.ADMISSION_BED_ASSIGNED.equals(statusCode)) {
            target.setAssignedWardCode(wardCode);
        }
        target.setUpdatedAt(LocalDateTime.now());
        releaseBedsIfDischarged(target.getDisposition().getReceptionId());
        return toAdmissionDto(target);
    }

    /**
     * 퇴실 처리가 끝나면(DischargeProgress 기준 DONE) 응급실 병상을 자동으로 비운다(해제자 SYSTEM, 병상은 EMPTY).
     * 귀가·사망·자의퇴원은 결정 즉시, 입원은 병동 병상 배정 회신, 전원은 소견서 작성 시점이다.
     */
    private void releaseBedsIfDischarged(String receptionId) {
        if (!StringUtils.hasText(receptionId)) {
            return;
        }
        if (dischargeProgress.stage(receptionId) == DischargeProgress.Stage.DONE) {
            resourceService.releaseBedsOf(receptionId, ResourceService.SYSTEM_ACTOR);
        }
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
        dto.setAssignedWardCode(saved.getAssignedWardCode());
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

    private java.util.Optional<Disposition> latestDisposition(String receptionId) {
        return dispositionRepository.findByReceptionIdOrderByDecidedAtDesc(receptionId).stream().findFirst();
    }

    /** 결정을 바꾼 뒤에는 이전 결정에 입원요청·전원 소견서를 붙일 수 없다 */
    private void requireLatest(Disposition disposition) {
        boolean latest = latestDisposition(disposition.getReceptionId())
                .map(d -> d.getId().equals(disposition.getId())).orElse(true);
        if (!latest) {
            throw new ConflictException("disposition has been replaced by a newer decision: " + disposition.getId());
        }
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
