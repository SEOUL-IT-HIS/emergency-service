package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionCancellableDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ConsentRecordRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository;
import kr.co.seoulit.his.emergencyservice.care.service.ReceptionCancellationService.Result;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCorePrescription;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.EmsReferralRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.EwsRecordRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.IsolationAssessmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.RiskScreeningRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 접수 취소 - 기록이 없으면 취소 시각만 남기고, 기록이 있으면 거절한다 */
class ReceptionCancellationServiceTest {

    private static final LocalDateTime CANCELLED = LocalDateTime.of(2026, 10, 5, 11, 0);

    private ReceptionIntakeRepository receptions;
    private ClinicalNoteRepository notes;
    private BedAssignmentRepository beds;
    private TriageAssessmentRepository triage;
    private OrderCoreClient orders;
    private ReceptionCancellationService service;
    private ReceptionIntake intake;

    @BeforeEach
    void setUp() {
        receptions = mock(ReceptionIntakeRepository.class);
        notes = mock(ClinicalNoteRepository.class);
        beds = mock(BedAssignmentRepository.class);
        triage = mock(TriageAssessmentRepository.class);
        orders = mock(OrderCoreClient.class);
        service = new ReceptionCancellationService(receptions, notes, mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                mock(ConsentRecordRepository.class), triage, mock(EwsRecordRepository.class),
                mock(IsolationAssessmentRepository.class), mock(RiskScreeningRepository.class),
                mock(EmsReferralRepository.class), beds, mock(DispositionRepository.class), orders);
        intake = new ReceptionIntake();
        intake.setId("r-1");
        when(receptions.findById("r-1")).thenReturn(Optional.of(intake));
        when(triage.findByReceptionId("r-1")).thenReturn(List.of());
        when(orders.listByReception("r-1")).thenReturn(List.of());
    }

    @Test
    void 기록이_없으면_취소_시각을_남긴다() {
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.CANCELLED);
        assertThat(intake.getCancelledAt()).isEqualTo(CANCELLED);
        assertThat(intake.isCancelled()).isTrue();
        verify(receptions).save(intake);
    }

    @Test
    void 취소_시각을_모르면_지금으로_남긴다() {
        assertThat(service.cancel("r-1", null)).isEqualTo(Result.CANCELLED);
        assertThat(intake.getCancelledAt()).isNotNull();
    }

    @Test
    void 접수가_넣어준_KTAS는_기록으로_보지_않는다() {
        TriageAssessment fromReception = new TriageAssessment();
        fromReception.setAssessedById(CareServiceImpl.RECEPTION_ASSESSOR);
        when(triage.findByReceptionId("r-1")).thenReturn(List.of(fromReception));
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.CANCELLED);
    }

    @Test
    void 직원이_입력한_KTAS가_있으면_거절한다() {
        TriageAssessment byNurse = new TriageAssessment();
        byNurse.setAssessedById("nurse-1");
        when(triage.findByReceptionId("r-1")).thenReturn(List.of(byNurse));
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.REFUSED_HAS_RECORDS);
        assertThat(intake.isCancelled()).isFalse();
        verify(receptions, never()).save(any());
    }

    @Test
    void 진료기록이_있으면_거절한다() {
        when(notes.existsByReceptionId("r-1")).thenReturn(true);
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.REFUSED_HAS_RECORDS);
        assertThat(intake.isCancelled()).isFalse();
    }

    @Test
    void 지금_병상에_배정돼_있으면_거절한다() {
        when(beds.existsByReceptionIdAndReleasedAtIsNull("r-1")).thenReturn(true);
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.REFUSED_HAS_RECORDS);
    }

    @Test
    void 처방이_있으면_거절한다() {
        when(orders.listByReception("r-1")).thenReturn(List.of(new OrderCorePrescription()));
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.REFUSED_HAS_RECORDS);
        assertThat(intake.isCancelled()).isFalse();
    }

    @Test
    void 처방코어를_못_읽으면_취소하지_않는다() {
        when(orders.listByReception("r-1")).thenThrow(new IllegalStateException("처방코어 연결 실패"));
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.REFUSED_CANNOT_VERIFY);
        assertThat(intake.isCancelled()).isFalse();
    }

    @Test
    void 같은_취소가_다시_와도_취소_시각을_덮어쓰지_않는다() {
        service.cancel("r-1", CANCELLED);
        assertThat(service.cancel("r-1", CANCELLED.plusHours(1))).isEqualTo(Result.ALREADY_CANCELLED);
        assertThat(intake.getCancelledAt()).isEqualTo(CANCELLED);
    }

    @Test
    void 응급에_없는_접수는_건너뛴다() {
        when(receptions.findById("none")).thenReturn(Optional.empty());
        assertThat(service.cancel("none", CANCELLED)).isEqualTo(Result.NOT_FOUND);
        verify(receptions, never()).save(any());
    }

    @Test
    void checkSaysCancellableWhenThereAreNoRecordsAndChangesNothing() {
        ReceptionCancellableDto dto = service.check("r-1");

        assertThat(dto.isCancellable()).isTrue();
        assertThat(dto.getReasonCode()).isEqualTo("CANCELLABLE");
        assertThat(dto.getRecords()).isEmpty();
        assertThat(intake.isCancelled()).as("조회는 아무것도 바꾸지 않는다").isFalse();
        verify(receptions, never()).save(any());
    }

    @Test
    void checkListsEveryKindOfRecordItFinds() {
        when(notes.existsByReceptionId("r-1")).thenReturn(true);
        when(beds.existsByReceptionIdAndReleasedAtIsNull("r-1")).thenReturn(true);
        TriageAssessment byNurse = new TriageAssessment();
        byNurse.setAssessedById("nurse-1");
        when(triage.findByReceptionId("r-1")).thenReturn(List.of(byNurse));

        ReceptionCancellableDto dto = service.check("r-1");

        assertThat(dto.isCancellable()).isFalse();
        assertThat(dto.getReasonCode()).isEqualTo("HAS_RECORDS");
        assertThat(dto.getRecords()).containsExactly("CLINICAL_NOTE", "KTAS", "BED_ASSIGNMENT");
    }

    @Test
    void checkReportsAnOrderAsARecord() {
        when(orders.listByReception("r-1")).thenReturn(List.of(new OrderCorePrescription()));

        ReceptionCancellableDto dto = service.check("r-1");

        assertThat(dto.isCancellable()).isFalse();
        assertThat(dto.getRecords()).containsExactly("ORDER");
    }

    @Test
    void checkIsNotCancellableWhenTheOrderCoreCannotBeRead() {
        when(orders.listByReception("r-1")).thenThrow(new IllegalStateException("처방코어 연결 실패"));

        ReceptionCancellableDto dto = service.check("r-1");

        assertThat(dto.isCancellable()).isFalse();
        assertThat(dto.getReasonCode()).isEqualTo("CANNOT_VERIFY");
    }

    @Test
    void checkTreatsAlreadyCancelledAndUnknownReceptionsAsCancellable() {
        intake.setCancelledAt(CANCELLED);
        assertThat(service.check("r-1").getReasonCode()).isEqualTo("ALREADY_CANCELLED");
        assertThat(service.check("r-1").isCancellable()).isTrue();

        when(receptions.findById("none")).thenReturn(Optional.empty());
        ReceptionCancellableDto unknown = service.check("none");
        assertThat(unknown.getReasonCode()).isEqualTo("NOT_FOUND");
        assertThat(unknown.isCancellable()).isTrue();
    }

    @Test
    void checkRequiresAReceptionId() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.check(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 병상을_배정했다가_해제했으면_기록으로_보지_않고_취소한다() {
        // 해제 이력은 남아 있어도(existsByReceptionId 는 true 가 될 수 있다) 지금 배정 중이 아니면 막지 않는다
        when(beds.existsByReceptionIdAndReleasedAtIsNull("r-1")).thenReturn(false);

        assertThat(service.check("r-1").isCancellable()).isTrue();
        assertThat(service.cancel("r-1", CANCELLED)).isEqualTo(Result.CANCELLED);
    }
}
