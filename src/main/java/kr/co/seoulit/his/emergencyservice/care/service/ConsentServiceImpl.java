package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ConsentRecord;
import kr.co.seoulit.his.emergencyservice.care.repository.ConsentRecordRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 동의 기록 (UML "동의 기록", Jira UD2-25).
 * 동의서는 현실에서 종이로 받고 시스템에는 수령 사실만 남긴다. 서명 요청·양식 조회·유예 처리 워크플로는 없다.
 */
@Service
@RequiredArgsConstructor
public class ConsentServiceImpl implements ConsentService {

    // admin 공통코드 그룹. admin 캐시에 그룹이 없으면 아래 폴백 값을 쓴다(admin 반영 후에는 코드 수정 없이 전환).

    // 값 자체로 분기하는 규칙(유예면 사유 필수, 보호자면 이름 필수)에 쓰는 값
    private static final String STATUS_DEFERRED = EmgCodes.CONSENT_STATUS_DEFERRED;
    private static final String BY_GUARDIAN = EmgCodes.CONSENT_BY_GUARDIAN;

    private static final int NAME_MAX = 100;
    private static final int REASON_MAX = 500;
    /** 서버-화면 시계 차이 허용치. 이보다 미래의 수령 일시는 잘못된 입력으로 본다. */
    private static final long FUTURE_TOLERANCE_MINUTES = 5;

    private final ConsentRecordRepository consentRecordRepository;
    private final CommonCodeResolver codeResolver;
    private final DischargeProgress dischargeProgress;

    @Override
    @Transactional
    public ConsentRecordDto createConsent(ConsentRecordCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getConsentTypeCode())
                || !StringUtils.hasText(request.getConsentStatusCode())
                || !StringUtils.hasText(request.getConsentedByCode())
                || !StringUtils.hasText(request.getRecordedById())) {
            throw new IllegalArgumentException(
                    "encounterId, consentTypeCode, consentStatusCode, consentedByCode, recordedById are required");
        }
        dischargeProgress.requireNotCancelled(request.getEncounterId());
        // 동의서 종류는 admin CONSENT_TYPE_CD 중 응급이 쓰기로 한 01·02·05만 허용(수술·마취·침습적 시술)
        Set<String> validTypes = codeResolver.valueSet(EmgCodes.CONSENT_TYPE_GROUP, EmgCodes.CONSENT_TYPE_ALLOWED);
        validTypes.retainAll(EmgCodes.CONSENT_TYPE_ALLOWED);
        codeResolver.require("consentTypeCode", request.getConsentTypeCode(), validTypes);
        codeResolver.require("consentStatusCode", request.getConsentStatusCode(),
                codeResolver.valueSet(EmgCodes.CONSENT_STATUS_GROUP, EmgCodes.CONSENT_STATUS_FALLBACK));
        codeResolver.require("consentedByCode", request.getConsentedByCode(),
                codeResolver.valueSet(EmgCodes.CONSENT_BY_GROUP, EmgCodes.CONSENT_BY_FALLBACK));

        if (STATUS_DEFERRED.equals(request.getConsentStatusCode()) && !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("reason is required when consentStatusCode is DEFERRED");
        }
        if (BY_GUARDIAN.equals(request.getConsentedByCode()) && !StringUtils.hasText(request.getConsenterName())) {
            throw new IllegalArgumentException("consenterName is required when consentedByCode is GUARDIAN");
        }
        if (request.getConsenterName() != null && request.getConsenterName().trim().length() > NAME_MAX) {
            throw new IllegalArgumentException("consenterName must be at most " + NAME_MAX + " characters");
        }
        if (request.getReason() != null && request.getReason().trim().length() > REASON_MAX) {
            throw new IllegalArgumentException("reason must be at most " + REASON_MAX + " characters");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime receivedAt = request.getReceivedAt() != null ? request.getReceivedAt() : now;
        if (receivedAt.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new IllegalArgumentException("receivedAt must not be in the future");
        }
        // 접수 시각보다 이를 수 없고, 귀가·사망·자의퇴원이면 퇴실 결정 시각보다 늦을 수 없다(요청에 시각이 있을 때만 확인)
        dischargeProgress.requireEventTimeDuringStay(request.getEncounterId(), request.getReceivedAt(), "receivedAt");

        ConsentRecord entity = new ConsentRecord();
        entity.setReceptionId(request.getEncounterId());
        entity.setConsentTypeCode(request.getConsentTypeCode());
        entity.setConsentStatusCode(request.getConsentStatusCode());
        entity.setConsentedByCode(request.getConsentedByCode());
        entity.setConsenterName(StringUtils.hasText(request.getConsenterName()) ? request.getConsenterName().trim() : null);
        entity.setReason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null);
        entity.setReceivedAt(receivedAt);
        entity.setRecordedById(request.getRecordedById());
        entity.setRecordedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(consentRecordRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentRecordDto> getConsents(String receptionId) {
        if (!StringUtils.hasText(receptionId)) {
            throw new IllegalArgumentException("receptionId is required");
        }
        return consentRecordRepository.findByReceptionIdOrderByReceivedAtDescRecordedAtDesc(receptionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ConsentRecordDto toDto(ConsentRecord entity) {
        ConsentRecordDto dto = new ConsentRecordDto();
        dto.setId(entity.getId());
        dto.setReceptionId(entity.getReceptionId());
        dto.setConsentTypeCode(entity.getConsentTypeCode());
        dto.setConsentStatusCode(entity.getConsentStatusCode());
        dto.setConsentedByCode(entity.getConsentedByCode());
        dto.setConsenterName(entity.getConsenterName());
        dto.setReason(entity.getReason());
        dto.setReceivedAt(entity.getReceivedAt());
        dto.setRecordedById(entity.getRecordedById());
        dto.setRecordedAt(entity.getRecordedAt());
        return dto;
    }
}
