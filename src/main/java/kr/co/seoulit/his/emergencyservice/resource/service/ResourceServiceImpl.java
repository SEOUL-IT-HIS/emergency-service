package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.*;
import kr.co.seoulit.his.emergencyservice.resource.entity.*;
import kr.co.seoulit.his.emergencyservice.resource.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private static final String ZONE_GROUP_CODE = "ZONE";
    // admin 공통코드 ZONE 그룹이 아직 없을 때 쓰는 기본 구역 (Jira UD2-81 기준)
    private static final List<String> ZONE_FALLBACK =
            List.of("RESUS", "CRITICAL", "URGENT", "FAST_TRACK", "PEDIATRIC", "ISOLATION");
    private static final String UNASSIGNED_ZONE = "UNASSIGNED";

    private static final String STATUS_EMPTY = "EMPTY";
    private static final String STATUS_OCCUPIED = "OCCUPIED";
    private static final String STATUS_CLEANING = "CLEANING";
    private static final String STATUS_OUT_OF_SERVICE = "OUT_OF_SERVICE";
    private static final String LEVEL_NO_BEDS = "NO_BEDS";

    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final CommonCodeCache commonCodeCache;

    /**
     * 혼잡도 집계 규칙 (UD2-84)
     * - 가용 = EMPTY만. 운영 병상 = 전체 - OUT_OF_SERVICE.
     * - 혼잡도(%) = (운영 - 가용) / 운영 x 100 → 점유뿐 아니라 청소중·상태미정도 "지금 못 쓰는 병상"으로 본다.
     * - 상태가 null 이거나 정의 외 값이면 unknownStatusBeds 로 세고, 운영 병상에는 포함(가용 아님, 보수적).
     * - 구역 목록 = ZONE 공통코드(없으면 폴백) + 코드에 없지만 병상이 있는 구역 + 구역 미지정(UNASSIGNED).
     *   병상이 0개인 코드 구역도 NO_BEDS 로 내려줘서 화면이 "병상 없음"을 표시할 수 있게 한다(UD2-85).
     * - 전체 지표는 구역별 값을 더해서 만든다 → 구역 합 = 전체 가 항상 성립.
     */
    @Override
    @Transactional(readOnly = true)
    public CongestionDto getCongestion() {
        Map<String, CongestionMetricDto> byZone = new LinkedHashMap<>();
        for (String zoneCode : zoneCodes()) {
            byZone.put(zoneCode, emptyMetric(zoneCode));
        }

        for (BedStatusCount row : bedRepository.countGroupByZoneAndStatus()) {
            String zoneCode = StringUtils.hasText(row.getZoneCode()) ? row.getZoneCode() : UNASSIGNED_ZONE;
            CongestionMetricDto metric = byZone.computeIfAbsent(zoneCode, this::emptyMetric);
            addCount(metric, row.getBedStatusCode(), row.getBedCount() == null ? 0 : row.getBedCount());
        }

        CongestionMetricDto total = emptyMetric(null);
        for (CongestionMetricDto zone : byZone.values()) {
            finishMetric(zone);
            total.setTotalBeds(total.getTotalBeds() + zone.getTotalBeds());
            total.setOccupiedBeds(total.getOccupiedBeds() + zone.getOccupiedBeds());
            total.setAvailableBeds(total.getAvailableBeds() + zone.getAvailableBeds());
            total.setCleaningBeds(total.getCleaningBeds() + zone.getCleaningBeds());
            total.setOutOfServiceBeds(total.getOutOfServiceBeds() + zone.getOutOfServiceBeds());
            total.setUnknownStatusBeds(total.getUnknownStatusBeds() + zone.getUnknownStatusBeds());
        }
        finishMetric(total);

        CongestionDto dto = new CongestionDto();
        dto.setTotal(total);
        dto.setZones(new ArrayList<>(byZone.values()));
        dto.setCalculatedAt(LocalDateTime.now());
        return dto;
    }

    private List<String> zoneCodes() {
        List<AdminCommonCodeItemDto> codes = commonCodeCache.get(ZONE_GROUP_CODE);
        if (codes.isEmpty()) {
            return ZONE_FALLBACK;
        }
        return codes.stream()
                .filter(code -> !"N".equals(code.getUseYn()))
                .map(AdminCommonCodeItemDto::getCodeValue)
                .collect(Collectors.toList());
    }

    private CongestionMetricDto emptyMetric(String zoneCode) {
        CongestionMetricDto metric = new CongestionMetricDto();
        metric.setZoneCode(zoneCode);
        return metric;
    }

    private void addCount(CongestionMetricDto metric, String statusCode, long count) {
        metric.setTotalBeds(metric.getTotalBeds() + count);
        if (STATUS_OCCUPIED.equals(statusCode)) {
            metric.setOccupiedBeds(metric.getOccupiedBeds() + count);
        } else if (STATUS_EMPTY.equals(statusCode)) {
            metric.setAvailableBeds(metric.getAvailableBeds() + count);
        } else if (STATUS_CLEANING.equals(statusCode)) {
            metric.setCleaningBeds(metric.getCleaningBeds() + count);
        } else if (STATUS_OUT_OF_SERVICE.equals(statusCode)) {
            metric.setOutOfServiceBeds(metric.getOutOfServiceBeds() + count);
        } else {
            metric.setUnknownStatusBeds(metric.getUnknownStatusBeds() + count);
        }
    }

    private void finishMetric(CongestionMetricDto metric) {
        long operational = metric.getTotalBeds() - metric.getOutOfServiceBeds();
        metric.setOperationalBeds(operational);
        if (operational <= 0) {
            metric.setCongestionRate(null);
            metric.setCongestionLevel(LEVEL_NO_BEDS);
            return;
        }
        double rate = (operational - metric.getAvailableBeds()) * 100.0 / operational;
        metric.setCongestionRate(Math.round(rate * 10) / 10.0);
        metric.setCongestionLevel(congestionLevel(rate));
    }

    /** 등급 구간 (UD2-83): 여유 <50, 보통 50~75 미만, 혼잡 75~90 미만, 포화 90 이상 */
    private String congestionLevel(double rate) {
        if (rate >= 90) {
            return "SATURATED";
        }
        if (rate >= 75) {
            return "HIGH";
        }
        if (rate >= 50) {
            return "MODERATE";
        }
        return "LOW";
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
    public BedAssignmentDto releaseBed(String assignmentId, BedReleaseRequestDto request) {
        if (!StringUtils.hasText(request.getReleasedById())) {
            throw new IllegalArgumentException("releasedById is required");
        }
        BedAssignment assignment = bedAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("bedAssignment", assignmentId));
        if (assignment.getReleasedAt() != null) {
            throw new ConflictException("bed assignment already released: " + assignmentId);
        }
        assignment.setReleasedById(request.getReleasedById());
        assignment.setReleasedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        Bed bed = assignment.getBed();
        bed.setBedStatusCode("EMPTY");
        bed.setUpdatedAt(LocalDateTime.now());

        BedAssignmentDto dto = new BedAssignmentDto();
        dto.setId(assignment.getId());
        dto.setReceptionId(assignment.getReceptionId());
        dto.setBedId(bed.getId());
        dto.setBedNo(bed.getBedNo());
        dto.setZoneCode(bed.getZoneCode());
        dto.setAssignedById(assignment.getAssignedById());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setReleasedById(assignment.getReleasedById());
        dto.setReleasedAt(assignment.getReleasedAt());
        return dto;
    }
}
