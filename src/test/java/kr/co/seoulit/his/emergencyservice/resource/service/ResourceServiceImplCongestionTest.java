package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
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
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(row(EmgCodes.ZONE_URGENT, EmgCodes.BED_STATUS_OCCUPIED, 2)));

        CongestionDto dto = service.getCongestion();

        assertThat(dto.getZones()).extracting(CongestionMetricDto::getZoneCode)
                .containsExactly(EmgCodes.ZONE_RESUS, EmgCodes.ZONE_CRITICAL, EmgCodes.ZONE_URGENT, EmgCodes.ZONE_FAST_TRACK, EmgCodes.ZONE_PEDIATRIC, EmgCodes.ZONE_ISOLATION);
        CongestionMetricDto resus = zone(dto, EmgCodes.ZONE_RESUS);
        assertThat(resus.getTotalBeds()).isZero();
        assertThat(resus.getCongestionRate()).isNull();
        assertThat(resus.getCongestionLevel()).isEqualTo("NO_BEDS");
    }

    @Test
    void cleaningAndUnknownCountAsUnavailableButOutOfServiceIsExcludedFromCapacity() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(
                row(EmgCodes.ZONE_URGENT, EmgCodes.BED_STATUS_OCCUPIED, 1),
                row(EmgCodes.ZONE_URGENT, EmgCodes.BED_STATUS_CLEANING, 1),
                row(EmgCodes.ZONE_URGENT, EmgCodes.BED_STATUS_EMPTY, 1),
                row(EmgCodes.ZONE_URGENT, EmgCodes.BED_STATUS_OUT_OF_SERVICE, 1),
                row(EmgCodes.ZONE_URGENT, null, 1)));

        CongestionMetricDto urgent = zone(service.getCongestion(), EmgCodes.ZONE_URGENT);

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
                row(EmgCodes.ZONE_RESUS, EmgCodes.BED_STATUS_OCCUPIED, 1), row(EmgCodes.ZONE_RESUS, EmgCodes.BED_STATUS_EMPTY, 1),          // 50%
                row(EmgCodes.ZONE_CRITICAL, EmgCodes.BED_STATUS_OCCUPIED, 9), row(EmgCodes.ZONE_CRITICAL, EmgCodes.BED_STATUS_EMPTY, 1),    // 90%
                row(EmgCodes.ZONE_PEDIATRIC, EmgCodes.BED_STATUS_OCCUPIED, 49), row(EmgCodes.ZONE_PEDIATRIC, EmgCodes.BED_STATUS_EMPTY, 51) // 49%
        ));

        CongestionDto dto = service.getCongestion();

        assertThat(zone(dto, EmgCodes.ZONE_RESUS).getCongestionLevel()).isEqualTo("MODERATE");
        assertThat(zone(dto, EmgCodes.ZONE_CRITICAL).getCongestionLevel()).isEqualTo("SATURATED");
        assertThat(zone(dto, EmgCodes.ZONE_PEDIATRIC).getCongestionLevel()).isEqualTo("LOW");
    }

    @Test
    void unknownZoneAndMissingZoneAreKeptSoTotalsMatchZoneSums() {
        when(bedRepository.countGroupByZoneAndStatus()).thenReturn(List.of(
                row(EmgCodes.ZONE_CRITICAL, EmgCodes.BED_STATUS_OCCUPIED, 1),
                row(EmgCodes.ZONE_ISOLATION, EmgCodes.BED_STATUS_EMPTY, 4),
                row("OBSERVATION", EmgCodes.BED_STATUS_EMPTY, 2),
                row(null, EmgCodes.BED_STATUS_OCCUPIED, 1)));

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
