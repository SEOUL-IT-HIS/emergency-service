package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.EmergencyPatientDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** 환자 목록이 KTAS·병상을 환자마다 따로 조회하지 않고(N+1) 한 번씩만 조회하는지 */
class CarePatientListQueryTest {

    private TriageAssessmentRepository triageAssessmentRepository;
    private BedAssignmentRepository bedAssignmentRepository;
    private ReceptionIntakeRepository receptionIntakeRepository;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        triageAssessmentRepository = mock(TriageAssessmentRepository.class);
        bedAssignmentRepository = mock(BedAssignmentRepository.class);
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        DischargeProgress dischargeProgress = mock(DischargeProgress.class);
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of());
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                triageAssessmentRepository, bedAssignmentRepository, receptionIntakeRepository, cache,
                new CommonCodeResolver(cache), mock(CareMapstructMapper.class), mock(PatientClient.class),
                dischargeProgress);

        LocalDateTime today = LocalDateTime.of(2026, 10, 1, 9, 0);
        when(receptionIntakeRepository.findAll()).thenReturn(List.of(
                intake("r1", today), intake("r2", today), intake("r3", today), intake("old", today.minusDays(1))));
        when(triageAssessmentRepository.findByReceptionIdInOrderByAssessedAtAsc(anyCollection())).thenReturn(List.of(
                triage("r1", "03", today.plusMinutes(1)), triage("r1", "02", today.plusMinutes(30)),  // 오래된 순
                triage("r2", "05", today.plusMinutes(5))));
        when(bedAssignmentRepository.findActiveWithBedByReceptionIdIn(anyCollection()))
                .thenReturn(List.of(assignment("r1", "BED-101", "01")));
    }

    private ReceptionIntake intake(String id, LocalDateTime receivedAt) {
        ReceptionIntake r = new ReceptionIntake();
        r.setId(id);
        r.setReceivedAt(receivedAt);
        return r;
    }

    private TriageAssessment triage(String receptionId, String ktas, LocalDateTime at) {
        TriageAssessment t = new TriageAssessment();
        t.setReceptionId(receptionId);
        t.setKtasLevelCode(ktas);
        t.setAssessedAt(at);
        return t;
    }

    private BedAssignment assignment(String receptionId, String bedNo, String zone) {
        Bed bed = new Bed();
        bed.setBedNo(bedNo);
        bed.setZoneCode(zone);
        BedAssignment a = new BedAssignment();
        a.setReceptionId(receptionId);
        a.setBed(bed);
        return a;
    }

    @Test
    void triageAndBedAreLoadedOnceForTheWholeListAndMatchedPerPatient() {
        Map<String, EmergencyPatientDto> byId = service.getPatients(null, null).stream()
                .collect(Collectors.toMap(EmergencyPatientDto::getReceptionId, Function.identity()));

        // 환자가 4명이어도 KTAS·병상 조회는 한 번씩만
        verify(triageAssessmentRepository, times(1)).findByReceptionIdInOrderByAssessedAtAsc(anyCollection());
        verify(bedAssignmentRepository, times(1)).findActiveWithBedByReceptionIdIn(anyCollection());
        // 가장 최근 평가가 표시된다
        assertThat(byId.get("r1").getKtasLevelCode()).isEqualTo("02");
        assertThat(byId.get("r1").getBedNo()).isEqualTo("BED-101");
        assertThat(byId.get("r2").getKtasLevelCode()).isEqualTo("05");
        assertThat(byId.get("r2").getBedNo()).isNull();
        assertThat(byId.get("r3").getKtasLevelCode()).isNull();
    }

    @Test
    void patientsAreListedInReceptionOrderRegardlessOfRepositoryOrder() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 9, 0);
        // 저장소가 접수ID 순서도 접수 순서도 아닌 임의 순서로 돌려줘도
        when(receptionIntakeRepository.findAll()).thenReturn(List.of(
                intake("zzz", base.plusMinutes(2)), intake("aaa", base.plusMinutes(3)),
                intake("mmm", base.plusMinutes(1)), intake("nodate", null)));

        List<EmergencyPatientDto> result = service.getPatients(null, null);

        // 접수 시각 오름차순, 접수 시각이 없는 건은 맨 뒤
        assertThat(result).extracting(EmergencyPatientDto::getReceptionId)
                .containsExactly("mmm", "zzz", "aaa", "nodate");
    }

    @Test
    void dateFilterIsAppliedBeforeTheBatchLookups() {
        List<EmergencyPatientDto> result = service.getPatients("2026-10-01", null);

        assertThat(result).extracting(EmergencyPatientDto::getReceptionId).containsExactlyInAnyOrder("r1", "r2", "r3");
        // 다른 날짜 접수("old")는 조회 대상에서도 빠진다
        verify(triageAssessmentRepository).findByReceptionIdInOrderByAssessedAtAsc(
                argThat(ids -> ids.size() == 3 && !ids.contains("old")));
    }
}
