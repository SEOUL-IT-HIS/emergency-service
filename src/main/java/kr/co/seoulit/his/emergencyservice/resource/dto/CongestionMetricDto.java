package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 혼잡도 지표 한 묶음(구역 1개 또는 전체).
 * 집계 규칙은 ResourceServiceImpl.getCongestion() 주석 참고.
 */
@Getter
@Setter
public class CongestionMetricDto {
    /** 전체 합계 행이면 null */
    private String zoneCode;
    private long totalBeds;
    private long operationalBeds;
    private long occupiedBeds;
    private long availableBeds;
    private long cleaningBeds;
    private long outOfServiceBeds;
    private long unknownStatusBeds;
    /** 운영 병상이 0이면 null (나눌 수 없음) */
    private Double congestionRate;
    /** NO_BEDS / LOW / MODERATE / HIGH / SATURATED */
    private String congestionLevel;
}
