package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
import kr.co.seoulit.his.emergencyservice.order.service.OrderServiceImpl;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedAssignmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedRepository;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceServiceImpl;
import kr.co.seoulit.his.emergencyservice.triage.dto.IsolationCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.KtasCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.KtasUpdateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.RiskScreeningCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.VitalAssessmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.mapper.TriageMapstructMapper;
import kr.co.seoulit.his.emergencyservice.triage.repository.EmsReferralRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.EwsRecordRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.IsolationAssessmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.RiskScreeningRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.service.TriageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 퇴실 완료(DONE) 접수에는 새로 배치·평가·처방하지 못하고(409), 병동 대기·진료 중에는 그대로 된다 */
class DischargedReceptionGuardTest {

    private DispositionRepository dispositionRepository;
    private DischargeProgress dischargeProgress;
    private ResourceServiceImpl resourceService;
    private TriageServiceImpl triageService;
    private TriageAssessmentRepository triageAssessmentRepository;
    private OrderServiceImpl orderService;
    private OrderCoreClient orderCoreClient;

    @BeforeEach
    void setUp() {
        dispositionRepository = mock(DispositionRepository.class);
        dischargeProgress = new DischargeProgress(dispositionRepository, mock(AdmissionRequestRepository.class),
                mock(TransferNoteRepository.class), org.mockito.Mockito.mock(kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository.class));
        CommonCodeCache cache = new CommonCodeCache();
        resourceService = new ResourceServiceImpl(mock(BedRepository.class), mock(BedAssignmentRepository.class), cache, dischargeProgress);
        triageAssessmentRepository = mock(TriageAssessmentRepository.class);
        triageService = new TriageServiceImpl(mock(EmsReferralRepository.class), triageAssessmentRepository,
                mock(EwsRecordRepository.class), mock(IsolationAssessmentRepository.class), mock(RiskScreeningRepository.class),
                mock(TriageMapstructMapper.class), new CommonCodeResolver(cache), dischargeProgress);
        orderCoreClient = mock(OrderCoreClient.class);
        ReceptionIntakeRepository receptions = mock(ReceptionIntakeRepository.class);
        ReceptionIntake intake = new ReceptionIntake();
        intake.setId("r-1");
        intake.setPatientId("p-1");
        when(receptions.findById("r-1")).thenReturn(Optional.of(intake));
        orderService = new OrderServiceImpl(orderCoreClient, receptions, new CommonCodeResolver(cache), dischargeProgress, "10", true, true);
    }

    /** r-1 이 귀가로 퇴실 처리 완료(DONE)된 상태 */
    private void discharged() {
        Disposition home = new Disposition();
        home.setId("d-1");
        home.setReceptionId("r-1");
        home.setDispositionTypeCode(EmgCodes.DISPOSITION_HOME);
        home.setDecidedAt(LocalDateTime.now());
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(List.of(home));
    }

    @Test
    void bedCannotBeAssignedToADischargedPatient() {
        discharged();
        BedAssignmentCreateRequestDto request = new BedAssignmentCreateRequestDto();
        request.setEncounterId("r-1");
        request.setBedId("bed-1");

        assertThatThrownBy(() -> resourceService.assignBed(request))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already discharged");
    }

    @Test
    void triageEntriesAreRejectedForADischargedPatient() {
        discharged();
        KtasCreateRequestDto ktas = new KtasCreateRequestDto();
        ktas.setEncounterId("r-1");
        ktas.setKtasScore("03");
        ktas.setAssessedById("dr-1");
        assertThatThrownBy(() -> triageService.createKtas(ktas)).isInstanceOf(ConflictException.class);

        TriageAssessment previous = new TriageAssessment();
        previous.setReceptionId("r-1");
        previous.setKtasLevelCode("03");
        when(triageAssessmentRepository.findById("t-1")).thenReturn(Optional.of(previous));
        assertThatThrownBy(() -> triageService.updateKtas("t-1", new KtasUpdateRequestDto()))
                .isInstanceOf(ConflictException.class);

        VitalAssessmentCreateRequestDto vitals = new VitalAssessmentCreateRequestDto();
        vitals.setEncounterId("r-1");
        vitals.setMeasuredById("n-1");
        vitals.setVitals(List.of(new VitalAssessmentCreateRequestDto.VitalItemDto()));
        assertThatThrownBy(() -> triageService.createVitalAssessments(vitals)).isInstanceOf(ConflictException.class);

        IsolationCreateRequestDto isolation = new IsolationCreateRequestDto();
        isolation.setEncounterId("r-1");
        isolation.setIsolationTypeCode("01");
        isolation.setDecidedById("dr-1");
        assertThatThrownBy(() -> triageService.createIsolation(isolation)).isInstanceOf(ConflictException.class);

        RiskScreeningCreateRequestDto screening = new RiskScreeningCreateRequestDto();
        screening.setEncounterId("r-1");
        screening.setScreenType("01");
        screening.setScreenedById("dr-1");
        assertThatThrownBy(() -> triageService.createRiskScreening(screening)).isInstanceOf(ConflictException.class);
    }

    @Test
    void ordersCannotBeRegisteredForADischargedPatientAndTheOrderCoreIsNotCalled() {
        discharged();
        OrderItemDto item = new OrderItemDto();
        item.setPrescriptionType("검사");
        item.setItemCode("02");
        item.setItemName("CBC");
        OrderCreateRequestDto request = new OrderCreateRequestDto();
        request.setEncounterId("r-1");
        request.setPrescribedBy("dr-1");
        request.setPriorityCode("01");
        request.setTimingCode("03");
        request.setItems(List.of(item));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already discharged");
        verifyNoInteractions(orderCoreClient);
    }

    @Test
    void nothingIsBlockedWhileThePatientIsStillInTheEr() {
        // 결정이 없거나(NONE) 입원 요청 전(OPEN)·병동 대기 중이면 통과한다 — 검사는 퇴실 완료일 때만 막는다
        assertThatCode(() -> dischargeProgress.requireNotDischarged("r-1")).doesNotThrowAnyException();
        Disposition admit = new Disposition();
        admit.setId("d-2");
        admit.setReceptionId("r-1");
        admit.setDispositionTypeCode(EmgCodes.DISPOSITION_ADMIT);
        admit.setDecidedAt(LocalDateTime.now());
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(List.of(admit));
        assertThatCode(() -> dischargeProgress.requireNotDischarged("r-1")).doesNotThrowAnyException();
        assertThatCode(() -> dischargeProgress.requireNotDischarged(null)).doesNotThrowAnyException();
        assertThat(dischargeProgress.stage("r-1")).isEqualTo(DischargeProgress.Stage.OPEN);
    }
}
