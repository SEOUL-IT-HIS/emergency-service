package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedAssignmentDto;
import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 퇴실 완료 시 병상 자동 해제(SYSTEM, EMPTY)와 접수의 현재 배정 조회 */
class ResourceReleaseTest {

    private BedAssignmentRepository bedAssignmentRepository;
    private ResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        bedAssignmentRepository = mock(BedAssignmentRepository.class);
        service = new ResourceServiceImpl(mock(BedRepository.class), bedAssignmentRepository, new CommonCodeCache());
    }

    private BedAssignment assignment(String id, String receptionId, String bedNo) {
        Bed bed = new Bed();
        bed.setId("bed-" + bedNo);
        bed.setBedNo(bedNo);
        bed.setZoneCode("06");
        bed.setBedStatusCode(EmgCodes.BED_STATUS_OCCUPIED);
        BedAssignment a = new BedAssignment();
        a.setId(id);
        a.setReceptionId(receptionId);
        a.setBed(bed);
        a.setAssignedById("nurse-1");
        a.setAssignedAt(LocalDateTime.now().minusHours(2));
        return a;
    }

    @Test
    void releasingAReceptionFreesItsBedsAsEmptyAndRecordsSystemAsTheActor() {
        BedAssignment a = assignment("ba-1", "r-1", "I-01");
        when(bedAssignmentRepository.findActiveWithBedByReceptionId("r-1")).thenReturn(List.of(a));

        int released = service.releaseBedsOf("r-1", ResourceService.SYSTEM_ACTOR);

        assertThat(released).isEqualTo(1);
        assertThat(a.getReleasedById()).isEqualTo("SYSTEM");
        assertThat(a.getReleasedAt()).isNotNull();
        assertThat(a.getBed().getBedStatusCode()).isEqualTo(EmgCodes.BED_STATUS_EMPTY);
    }

    @Test
    void nothingToReleaseIsHarmless() {
        when(bedAssignmentRepository.findActiveWithBedByReceptionId("r-9")).thenReturn(List.of());

        assertThat(service.releaseBedsOf("r-9", ResourceService.SYSTEM_ACTOR)).isZero();
        assertThat(service.releaseBedsOf(" ", ResourceService.SYSTEM_ACTOR)).isZero();
    }

    @Test
    void currentAssignmentIsReturnedWithBedInfoOrNull() {
        when(bedAssignmentRepository.findActiveWithBedByReceptionId("r-1"))
                .thenReturn(List.of(assignment("ba-1", "r-1", "I-01")));
        when(bedAssignmentRepository.findActiveWithBedByReceptionId("r-2")).thenReturn(List.of());

        BedAssignmentDto dto = service.getCurrentAssignment("r-1");
        assertThat(dto.getId()).isEqualTo("ba-1");
        assertThat(dto.getBedNo()).isEqualTo("I-01");
        assertThat(dto.getZoneCode()).isEqualTo("06");
        assertThat(dto.getReleasedAt()).isNull();
        assertThat(service.getCurrentAssignment("r-2")).isNull();
        assertThatThrownBy(() -> service.getCurrentAssignment(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
