package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CongestionDto {
    /** 전체 지표 — 항상 zones 합산으로 계산하므로 구역 합과 일치한다. */
    private CongestionMetricDto total;
    private List<CongestionMetricDto> zones;
    private LocalDateTime calculatedAt;
}
