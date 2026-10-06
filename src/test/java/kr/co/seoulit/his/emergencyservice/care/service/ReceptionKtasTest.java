package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionIntakeCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 접수가 KTAS 등급을 같이 주면 최초(INITIAL) 분류로 저장한다 */
class ReceptionKtasTest {

    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 10, 5, 9, 0);

    private ReceptionIntakeRepository receptions;
    private TriageAssessmentRepository triage;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        receptions = mock(ReceptionIntakeRepository.class);
        triage = mock(TriageAssessmentRepository.class);
        when(receptions.save(any(ReceptionIntake.class))).thenAnswer(inv -> inv.getArgument(0));
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class), triage,
                mock(BedAssignmentRepository.class), receptions, cache, new CommonCodeResolver(cache),
                mock(CareMapstructMapper.class), mock(PatientClient.class), mock(DischargeProgress.class));
    }

    private ReceptionIntakeCreateRequestDto request(String ktasLevel) {
        ReceptionIntakeCreateRequestDto r = new ReceptionIntakeCreateRequestDto();
        r.setReceptionId("r-1");
        r.setPatientId("p-1");
        r.setArrivalPath("01");
        r.setReceivedAt(RECEIVED);
        r.setKtasLevel(ktasLevel);
        return r;
    }

    private TriageAssessment saved() {
        ArgumentCaptor<TriageAssessment> captor = ArgumentCaptor.forClass(TriageAssessment.class);
        verify(triage).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void aSingleDigitLevelFromReceptionIsStoredAsTheInitialClassification() {
        service.createReceptionIntake(request("2"));

        TriageAssessment a = saved();
        assertThat(a.getReceptionId()).isEqualTo("r-1");
        assertThat(a.getKtasLevelCode()).as("admin TRIAGE_CD 는 두 자리 코드").isEqualTo("02");
        assertThat(a.getAssessmentTypeCode()).isEqualTo(EmgCodes.ASSESSMENT_INITIAL);
        assertThat(a.getAssessedById()).isEqualTo("RECEPTION");
        assertThat(a.getAssessedAt()).as("분류 시각이 없으면 접수 시각").isEqualTo(RECEIVED);
    }

    @Test
    void theTriageTimeFromReceptionIsUsedWhenGiven() {
        ReceptionIntakeCreateRequestDto r = request("01");
        r.setTriageDateTime(RECEIVED.plusMinutes(3));

        service.createReceptionIntake(r);

        assertThat(saved().getAssessedAt()).isEqualTo(RECEIVED.plusMinutes(3));
        assertThat(saved().getKtasLevelCode()).isEqualTo("01");
    }

    @Test
    void anExistingInitialClassificationIsNotTouched() {
        when(triage.existsByReceptionIdAndAssessmentTypeCode("r-1", EmgCodes.ASSESSMENT_INITIAL)).thenReturn(true);

        service.createReceptionIntake(request("3"));

        verify(triage, never()).save(any(TriageAssessment.class));
    }

    @Test
    void anUnknownLevelNeverRejectsTheReception() {
        service.createReceptionIntake(request("9"));
        service.createReceptionIntake(request("abc"));
        service.createReceptionIntake(request(null));
        service.createReceptionIntake(request("  "));

        verify(triage, never()).save(any(TriageAssessment.class));
        verify(receptions, org.mockito.Mockito.times(4)).save(any(ReceptionIntake.class));
        verify(triage, never()).existsByReceptionIdAndAssessmentTypeCode(anyString(), anyString());
    }
}
