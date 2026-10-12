package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.EmergencyPatientDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientDto;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientNames;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 임시환자·신원미상 환자는 이름이 비어 있다. 그런 환자가 한 명 섞여도 나머지 환자의 이름은 채워져야 한다
 * (예전에는 Collectors.toMap 의 NullPointerException 때문에 목록 전체의 이름이 사라졌다).
 */
class CarePatientNamesTest {

    private PatientClient patientClient;
    private ReceptionIntakeRepository receptionIntakeRepository;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        patientClient = mock(PatientClient.class);
        DischargeProgress dischargeProgress = mock(DischargeProgress.class);
        when(dischargeProgress.doneReceptionIds(anyCollection())).thenReturn(Set.of());
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                mock(TriageAssessmentRepository.class), mock(BedAssignmentRepository.class), receptionIntakeRepository, cache,
                new CommonCodeResolver(cache), mock(CareMapstructMapper.class), patientClient, dischargeProgress);
        LocalDateTime at = LocalDateTime.of(2026, 10, 12, 9, 0);
        when(receptionIntakeRepository.findAll()).thenReturn(List.of(
                intake("r1", "11111111-1111-4111-8111-111111111111", at),
                intake("r2", "22222222-2222-4222-8222-222222222222", at.plusMinutes(1)),
                intake("r3", "33333333-3333-4333-8333-333333333333", at.plusMinutes(2))));
    }

    private static ReceptionIntake intake(String id, String patientId, LocalDateTime receivedAt) {
        ReceptionIntake r = new ReceptionIntake();
        r.setId(id);
        r.setPatientId(patientId);
        r.setReceivedAt(receivedAt);
        return r;
    }

    private static PatientDto patient(String id, String name) {
        PatientDto p = new PatientDto();
        p.setPatientId(id);
        p.setPatientName(name);
        return p;
    }

    @Test
    void 이름이_없는_환자가_섞여도_나머지_환자의_이름은_채운다() {
        when(patientClient.getPatients(anyList())).thenReturn(List.of(
                patient("11111111-1111-4111-8111-111111111111", "김하나"),
                patient("22222222-2222-4222-8222-222222222222", null),     // 임시환자(이름 없음)
                patient("33333333-3333-4333-8333-333333333333", "이둘")));

        Map<String, EmergencyPatientDto> byId = service.getPatients(null, null).stream()
                .collect(Collectors.toMap(EmergencyPatientDto::getReceptionId, Function.identity()));

        assertThat(byId.get("r1").getPatientName()).isEqualTo("김하나");
        assertThat(byId.get("r2").getPatientName()).isNull();
        assertThat(byId.get("r3").getPatientName()).isEqualTo("이둘");
    }

    @Test
    void 환자서비스가_실패하면_이름_없이_목록은_내려준다() {
        when(patientClient.getPatients(anyList())).thenThrow(new IllegalStateException("down"));

        List<EmergencyPatientDto> result = service.getPatients(null, null);

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(dto -> assertThat(dto.getPatientName()).isNull());
    }

    @Test
    void 이름_맵은_이름이_비었거나_id가_없는_항목과_중복을_건너뛴다() {
        List<PatientDto> patients = new ArrayList<>();
        patients.add(patient("a", "에이"));
        patients.add(patient("a", "에이 중복"));
        patients.add(patient("b", null));
        patients.add(patient("c", "  "));
        patients.add(patient(null, "아이디없음"));
        patients.add(null);

        assertThat(PatientNames.byId(patients)).containsOnly(Map.entry("a", "에이"));
        assertThat(PatientNames.byId(null)).isEmpty();
    }
}
