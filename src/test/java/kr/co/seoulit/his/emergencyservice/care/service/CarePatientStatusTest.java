package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.EmergencyPatientDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.entity.TransferNote;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 접수 목록의 진료 상태(IN_CARE / DONE)와 상태 필터 — 퇴실 처리 진행에 따라 계산 */
class CarePatientStatusTest {

    private DispositionRepository dispositionRepository;
    private AdmissionRequestRepository admissionRequestRepository;
    private TransferNoteRepository transferNoteRepository;
    private ReceptionIntakeRepository receptionIntakeRepository;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        dispositionRepository = mock(DispositionRepository.class);
        admissionRequestRepository = mock(AdmissionRequestRepository.class);
        transferNoteRepository = mock(TransferNoteRepository.class);
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                mock(TriageAssessmentRepository.class), mock(BedAssignmentRepository.class),
                receptionIntakeRepository, cache, new CommonCodeResolver(cache), mock(CareMapstructMapper.class),
                mock(PatientClient.class),
                new DischargeProgress(dispositionRepository, admissionRequestRepository, transferNoteRepository));
        for (String id : List.of("none", "home", "admit-wait", "admit-ok", "admit-rejected", "tr-none", "tr-note")) {
            ReceptionIntake r = new ReceptionIntake();
            r.setId(id);
            r.setReceivedAt(LocalDateTime.now());
            when(receptionIntakeRepository.findAll()).thenReturn(intakes());
        }
        when(receptionIntakeRepository.findAll()).thenReturn(intakes());
        List<Disposition> ds = List.of(
                disposition("home", EmgCodes.DISPOSITION_HOME), disposition("admit-wait", EmgCodes.DISPOSITION_ADMIT),
                disposition("admit-ok", EmgCodes.DISPOSITION_ADMIT), disposition("admit-rejected", EmgCodes.DISPOSITION_ADMIT),
                disposition("tr-none", EmgCodes.DISPOSITION_TRANSFER), disposition("tr-note", EmgCodes.DISPOSITION_TRANSFER));
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(ds);
        when(admissionRequestRepository.findByDispositionIdIn(anyCollection())).thenReturn(List.of(
                request("admit-wait", EmgCodes.ADMISSION_REQUESTED), request("admit-ok", EmgCodes.ADMISSION_BED_ASSIGNED),
                request("admit-rejected", EmgCodes.ADMISSION_REJECTED)));
        TransferNote note = new TransferNote();
        note.setDisposition(ds.get(5));
        when(transferNoteRepository.findByDispositionIdIn(anyCollection())).thenReturn(List.of(note));
    }

    private List<ReceptionIntake> intakes() {
        return List.of("none", "home", "admit-wait", "admit-ok", "admit-rejected", "tr-none", "tr-note").stream().map(id -> {
            ReceptionIntake r = new ReceptionIntake();
            r.setId(id);
            r.setReceivedAt(LocalDateTime.now());
            return r;
        }).toList();
    }

    private Disposition disposition(String receptionId, String type) {
        Disposition d = new Disposition();
        d.setId("d-" + receptionId);
        d.setReceptionId(receptionId);
        d.setDispositionTypeCode(type);
        d.setDecidedAt(LocalDateTime.now());
        return d;
    }

    private AdmissionRequest request(String receptionId, String status) {
        AdmissionRequest a = new AdmissionRequest();
        Disposition d = new Disposition();
        d.setId("d-" + receptionId);
        a.setDisposition(d);
        a.setRequestStatusCode(status);
        a.setRequestedAt(LocalDateTime.now());
        return a;
    }

    private List<String> ids(String status) {
        return service.getPatients(null, status).stream().map(EmergencyPatientDto::getReceptionId).sorted().toList();
    }

    @Test
    void doneOnlyWhenTheDischargeProcessIsFinished() {
        // 귀가는 결정 즉시 완료, 입원은 병동이 병상을 배정해야 완료, 전원은 소견서를 써야 완료
        assertThat(ids(CareServiceImpl.CARE_STATUS_DONE)).containsExactly("admit-ok", "home", "tr-note");
    }

    @Test
    void everyoneElseStaysInCareIncludingWaitingAndRejectedAdmissions() {
        assertThat(ids(CareServiceImpl.CARE_STATUS_IN_CARE))
                .containsExactly("admit-rejected", "admit-wait", "none", "tr-none");
    }

    @Test
    void noStatusReturnsEveryone() {
        assertThat(ids(null)).hasSize(7);
    }
}
