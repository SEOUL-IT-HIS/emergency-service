package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ActiveReceptionDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 접수 서비스가 중복 접수를 경고하려고 호출하는 '환자의 진행 중인 응급 접수' 조회 */
class CareActiveReceptionTest {

    private ReceptionIntakeRepository receptionIntakeRepository;
    private DischargeProgress dischargeProgress;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        dischargeProgress = mock(DischargeProgress.class);
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                mock(TriageAssessmentRepository.class), mock(BedAssignmentRepository.class), receptionIntakeRepository,
                cache, new CommonCodeResolver(cache), mock(CareMapstructMapper.class), mock(PatientClient.class),
                dischargeProgress);
    }

    private ReceptionIntake intake(String id, String patientId, LocalDateTime receivedAt) {
        ReceptionIntake r = new ReceptionIntake();
        r.setId(id);
        r.setPatientId(patientId);
        r.setReceivedAt(receivedAt);
        return r;
    }

    @Test
    void returnsOnlyUnfinishedReceptionsOfThePatientOldestFirst() {
        LocalDateTime base = LocalDateTime.now().minusHours(5);
        when(receptionIntakeRepository.findByPatientId("p1")).thenReturn(List.of(
                intake("r-new", "p1", base.plusHours(2)),
                intake("r-done", "p1", base.plusHours(1)),
                intake("r-old", "p1", base)));
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of("r-done"));

        List<ActiveReceptionDto> result = service.getActiveReceptions("p1", null);

        // 퇴실 처리 끝난 접수는 빠지고, 접수 시각 오름차순
        assertThat(result).extracting(ActiveReceptionDto::getReceptionId).containsExactly("r-old", "r-new");
        assertThat(result.get(0).getPatientId()).isEqualTo("p1");
        assertThat(result.get(0).getReceivedAt()).isEqualTo(base);
    }

    @Test
    void receptionsOlderThanFortyEightHoursAreLeftOutByDefault() {
        LocalDateTime now = LocalDateTime.now();
        when(receptionIntakeRepository.findByPatientId("p1")).thenReturn(List.of(
                intake("stale", "p1", now.minusHours(49)),   // 퇴실 누락·테스트 데이터로 보고 제외
                intake("edge", "p1", now.minusHours(47)),
                intake("fresh", "p1", now.minusMinutes(10))));
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of());

        assertThat(service.getActiveReceptions("p1", null)).extracting(ActiveReceptionDto::getReceptionId)
                .containsExactly("edge", "fresh");
        assertThat(CareService.DEFAULT_ACTIVE_WINDOW_HOURS).isEqualTo(48);
    }

    @Test
    void sinceHoursOverridesTheDefaultWindow() {
        LocalDateTime now = LocalDateTime.now();
        when(receptionIntakeRepository.findByPatientId("p1")).thenReturn(List.of(
                intake("two-days", "p1", now.minusHours(40)),
                intake("fresh", "p1", now.minusHours(1))));
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of());

        assertThat(service.getActiveReceptions("p1", 24)).extracting(ActiveReceptionDto::getReceptionId)
                .containsExactly("fresh");
        assertThat(service.getActiveReceptions("p1", 72)).extracting(ActiveReceptionDto::getReceptionId)
                .containsExactly("two-days", "fresh");
    }

    @Test
    void emptyListWhenThePatientHasNoUnfinishedReception() {
        when(receptionIntakeRepository.findByPatientId("p2")).thenReturn(List.of());
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of());

        assertThat(service.getActiveReceptions("p2", null)).isEmpty();
    }

    @Test
    void blankPatientIdOrNonPositiveWindowIsRejected() {
        assertThatThrownBy(() -> service.getActiveReceptions(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patientId");
        assertThatThrownBy(() -> service.getActiveReceptions("p1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sinceHours");
    }
}
