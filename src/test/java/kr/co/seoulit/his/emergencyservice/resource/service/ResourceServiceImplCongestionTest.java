package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionMetricDto;
import kr.co.seoulit.his.emergencyservice.resource.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceServiceImplCongestionTest {

    private BedRepository bedRepository;
    private ResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        bedRepository = mock(BedRepository.class);
        service = new ResourceServiceImpl(
                bedRepository,
                mock(BedAssignmentRepository.class),
                mock(EquipmentRepository.class),
                mock(EquipmentAllocationRepository.class),
                new CommonCodeCache());
    }

    private static BedStatusCount row(String zone, String status, long count) {
        return new BedStatusCount() {
            public String getZoneCode() { return zone; }
            public String getBedStatusCode() { return status; }
            public Long getBedCount() { return count; }
        };
    }

    private static CongestionMetricDto zone(CongestionDto dto, String zoneCode) {
        return dto.getZones().stream().filter(z -> zoneCode.equals(z.getZoneCode())).findFirst().orElseThrow();
    }

    @Test
    void zonesWithoutBedsAreReturnedAsNoBeds() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(row("URGENT", "OCCUPIED", 2)));

        CongestionDto dto = service.getCongestion();

        assertThat(dto.getZones()).extracting(CongestionMetricDto::getZoneCode)
                .containsExactly("RESUS", "CRITICAL", "URGENT", "FAST_TRACK", "PEDIATRIC", "ISOLATION");
        CongestionMetricDto resus = zone(dto, "RESUS");
        assertThat(resus.getTotalBeds()).isZero();
        assertThat(resus.getCongestionRate()).isNull();
        assertThat(resus.getCongestionLevel()).isEqualTo("NO_BEDS");
    }

    @Test
    void cleaningAndUnknownCountAsUnavailableButOutOfServiceIsExcludedFromCapacity() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(
                row("URGENT", "OCCUPIED", 1),
                row("URGENT", "CLEANING", 1),
                row("URGENT", "EMPTY", 1),
                row("URGENT", "OUT_OF_SERVICE", 1),
                row("URGENT", null, 1)));

        CongestionMetricDto urgent = zone(service.getCongestion(), "URGENT");

        assertThat(urgent.getTotalBeds()).isEqualTo(5);
        assertThat(urgent.getOperationalBeds()).isEqualTo(4);
        assertThat(urgent.getAvailableBeds()).isEqualTo(1);
        assertThat(urgent.getUnknownStatusBeds()).isEqualTo(1);
        // (4 - 1) / 4 = 75%
        assertThat(urgent.getCongestionRate()).isEqualTo(75.0);
        assertThat(urgent.getCongestionLevel()).isEqualTo("HIGH");
    }

    @Test
    void levelBoundaries() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(
                row("RESUS", "OCCUPIED", 1), row("RESUS", "EMPTY", 1),          // 50%
                row("CRITICAL", "OCCUPIED", 9), row("CRITICAL", "EMPTY", 1),    // 90%
                row("PEDIATRIC", "OCCUPIED", 49), row("PEDIATRIC", "EMPTY", 51) // 49%
        ));

        CongestionDto dto = service.getCongestion();

        assertThat(zone(dto, "RESUS").getCongestionLevel()).isEqualTo("MODERATE");
        assertThat(zone(dto, "CRITICAL").getCongestionLevel()).isEqualTo("SATURATED");
        assertThat(zone(dto, "PEDIATRIC").getCongestionLevel()).isEqualTo("LOW");
    }

    @Test
    void unknownZoneAndMissingZoneAreKeptSoTotalsMatchZoneSums() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(
                row("CRITICAL", "OCCUPIED", 1),
                row("ISOLATION", "EMPTY", 4),
                row("OBSERVATION", "EMPTY", 2),
                row(null, "OCCUPIED", 1)));

        CongestionDto dto = service.getCongestion();

        assertThat(dto.getZones()).extracting(CongestionMetricDto::getZoneCode)
                .contains("OBSERVATION", "UNASSIGNED");
        long zoneSum = dto.getZones().stream().mapToLong(CongestionMetricDto::getTotalBeds).sum();
        long availableSum = dto.getZones().stream().mapToLong(CongestionMetricDto::getAvailableBeds).sum();
        assertThat(dto.getTotal().getTotalBeds()).isEqualTo(zoneSum).isEqualTo(8);
        assertThat(dto.getTotal().getAvailableBeds()).isEqualTo(availableSum).isEqualTo(6);
        assertThat(dto.getTotal().getZoneCode()).isNull();
    }

    @Test
    void noBedsAtAllDoesNotFail() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of());

        CongestionDto dto = service.getCongestion();

        assertThat(dto.getTotal().getTotalBeds()).isZero();
        assertThat(dto.getTotal().getCongestionLevel()).isEqualTo("NO_BEDS");
        assertThat(dto.getCalculatedAt()).isNotNull();
    }
}
