package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.monitor.dto.DashboardDto;
import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import kr.co.seoulit.his.emergencyservice.monitor.repository.LosAlertRepository;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionMetricDto;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 현황판 재실 수·장기체류 알림이 환자 목록과 같은 '퇴실 처리 완료' 기준을 쓰는지 */
class MonitorDischargeTest {

    private LosAlertRepository losAlertRepository;
    private ReceptionIntakeRepository receptionIntakeRepository;
    private PatientClient patientClient;
    private MonitorServiceImpl service;

    @BeforeEach
    void setUp() {
        losAlertRepository = mock(LosAlertRepository.class);
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        patientClient = mock(PatientClient.class);
        DispositionRepository dispositionRepository = mock(DispositionRepository.class);
        AdmissionRequestRepository admissionRequestRepository = mock(AdmissionRequestRepository.class);
        ResourceService resourceService = mock(ResourceService.class);
        CongestionDto congestion = mock(CongestionDto.class);
        when(congestion.getTotal()).thenReturn(new CongestionMetricDto());
        when(resourceService.getCongestion()).thenReturn(congestion);

        // 오래 전에 접수한 3명: 결정 없음 / 입원 결정 후 병상 대기 / 귀가
        List<ReceptionIntake> intakes = List.of(intake("none"), intake("admit-wait"), intake("home"));
        when(receptionIntakeRepository.findAll()).thenReturn(intakes);
        when(receptionIntakeRepository.findAllById(anyIterable())).thenReturn(intakes);
        Disposition admit = disposition("admit-wait", EmgCodes.DISPOSITION_ADMIT);
        when(dispositionRepository.findByReceptionIdIn(anyCollection()))
                .thenReturn(List.of(admit, disposition("home", EmgCodes.DISPOSITION_HOME)));
        AdmissionRequest waiting = new AdmissionRequest();
        waiting.setDisposition(admit);
        waiting.setRequestStatusCode(EmgCodes.ADMISSION_REQUESTED);
        waiting.setRequestedAt(LocalDateTime.now());
        when(admissionRequestRepository.findByDispositionIdIn(anyCollection())).thenReturn(List.of(waiting));

        service = new MonitorServiceImpl(losAlertRepository, receptionIntakeRepository,
                new DischargeProgress(dispositionRepository, admissionRequestRepository, mock(TransferNoteRepository.class),
                        org.mockito.Mockito.mock(kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository.class)),
                resourceService, patientClient);
    }

    private ReceptionIntake intake(String id) {
        ReceptionIntake r = new ReceptionIntake();
        r.setId(id);
        r.setReceivedAt(LocalDateTime.now().minusDays(1));
        return r;
    }

    private Disposition disposition(String receptionId, String type) {
        Disposition d = new Disposition();
        d.setId("d-" + receptionId);
        d.setReceptionId(receptionId);
        d.setDispositionTypeCode(type);
        d.setDecidedAt(LocalDateTime.now());
        return d;
    }

    private LosAlert alert(String receptionId) {
        LosAlert a = new LosAlert();
        a.setId("alert-" + receptionId);
        a.setReceptionId(receptionId);
        a.setThresholdMinutes(360);
        return a;
    }

    private List<String> savedAlertReceptionIds() {
        org.mockito.ArgumentCaptor<LosAlert> captor = org.mockito.ArgumentCaptor.forClass(LosAlert.class);
        verify(losAlertRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues().stream().map(LosAlert::getReceptionId).toList();
    }

    @Test
    void patientWaitingForAWardBedIsStillInTheErAndGetsALongStayAlert() {
        service.detectLongStayPatients();
        // 결정 없음 + 병상 대기 → 알림 생성, 귀가(완료)는 제외
        assertThat(savedAlertReceptionIds()).containsExactlyInAnyOrder("none", "admit-wait");
    }

    @Test
    void alreadyAlertedPatientsAreSkippedWithOneLookupInsteadOfOnePerPatient() {
        when(losAlertRepository.findReceptionIdsByThresholdMinutes(anyInt())).thenReturn(List.of("none"));

        service.detectLongStayPatients();

        assertThat(savedAlertReceptionIds()).containsExactly("admit-wait");
        // 이미 알린 접수 확인은 접수 수와 상관없이 한 번만 조회한다
        verify(losAlertRepository, times(1)).findReceptionIdsByThresholdMinutes(anyInt());
    }

    @Test
    void dashboardCountsUnfinishedPatientsAndHidesAlertsOfFinishedOnes() {
        when(losAlertRepository.findByAcknowledgedAtIsNull())
                .thenReturn(List.of(alert("none"), alert("admit-wait"), alert("home")));
        when(patientClient.getPatients(anyList())).thenThrow(new RuntimeException("patient-service down"));

        DashboardDto dto = service.getDashboard();

        assertThat(dto.getCurrentPatients()).isEqualTo(2);
        assertThat(dto.getOpenLosAlerts()).isEqualTo(2);
        // 환자서비스가 죽어도 현황판은 뜬다(이름만 비움)
        assertThat(dto.getRecentLosAlerts()).extracting("receptionId").containsExactly("none", "admit-wait");
    }
}
