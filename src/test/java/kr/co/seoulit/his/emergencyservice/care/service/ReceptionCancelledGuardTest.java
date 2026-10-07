package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.EmergencyPatientDto;
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
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 취소된 접수는 환자 목록·진행 중 접수에서 빠지고, 등록 이벤트가 다시 살리지 못한다 */
class ReceptionCancelledGuardTest {

    // 진행 중 접수 조회는 "접수 후 N시간 이내"만 보므로 고정 날짜를 쓰면 날이 지나 실패한다
    private static final LocalDateTime RECEIVED = LocalDateTime.now().minusHours(1);

    private ReceptionIntakeRepository receptions;
    private CareServiceImpl service;

    private static ReceptionIntake intake(String id, boolean cancelled) {
        ReceptionIntake i = new ReceptionIntake();
        i.setId(id);
        i.setPatientId("p-1");
        i.setReceivedAt(RECEIVED);
        if (cancelled) {
            i.setCancelledAt(RECEIVED.plusMinutes(5));
        }
        return i;
    }

    @BeforeEach
    void setUp() {
        receptions = mock(ReceptionIntakeRepository.class);
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(mock(ClinicalNoteRepository.class), mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class), mock(CprEventRepository.class),
                mock(TriageAssessmentRepository.class), mock(BedAssignmentRepository.class), receptions, cache,
                new CommonCodeResolver(cache), mock(CareMapstructMapper.class), mock(PatientClient.class),
                mock(DischargeProgress.class));
    }

    @Test
    void cancelledReceptionsAreOnlyInTheCancelledFilterAndAll() {
        when(receptions.findAll()).thenReturn(List.of(intake("r-ok", false), intake("r-cancelled", true)));

        assertThat(service.getPatients(null, "IN_CARE")).extracting(EmergencyPatientDto::getReceptionId)
                .containsExactly("r-ok");
        assertThat(service.getPatients(null, "DONE")).isEmpty();
        List<EmergencyPatientDto> cancelled = service.getPatients(null, "CANCELLED");
        assertThat(cancelled).extracting(EmergencyPatientDto::getReceptionId).containsExactly("r-cancelled");
        assertThat(cancelled.get(0).getCareStatusCode()).isEqualTo("CANCELLED");
        assertThat(service.getPatients(null, null)).extracting(EmergencyPatientDto::getReceptionId)
                .containsExactlyInAnyOrder("r-ok", "r-cancelled");
    }

    @Test
    void cancelledReceptionsAreNotReportedAsActiveToReception() {
        when(receptions.findByPatientId("p-1")).thenReturn(List.of(intake("r-ok", false), intake("r-cancelled", true)));

        assertThat(service.getActiveReceptions("p-1", 24)).extracting(a -> a.getReceptionId()).containsExactly("r-ok");
    }

    @Test
    void aRegistrationEventDoesNotReviveACancelledReception() {
        when(receptions.findById("r-cancelled")).thenReturn(Optional.of(intake("r-cancelled", true)));
        ReceptionIntakeCreateRequestDto r = new ReceptionIntakeCreateRequestDto();
        r.setReceptionId("r-cancelled");
        r.setPatientId("p-1");
        r.setArrivalPath("01");
        r.setReceivedAt(RECEIVED);

        assertThatThrownBy(() -> service.createReceptionIntake(r)).isInstanceOf(ConflictException.class);
        verify(receptions, never()).save(any(ReceptionIntake.class));
    }

    @Test
    void newEntriesOnACancelledReceptionAreRejected() {
        when(receptions.findById("r-cancelled")).thenReturn(Optional.of(intake("r-cancelled", true)));
        DischargeProgress guard = new DischargeProgress(
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository.class),
                receptions);

        assertThatThrownBy(() -> guard.requireNotDischarged("r-cancelled"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("reception cancelled");
    }

    // ---- 취소된 접수에는 진료기록·처치·투약·CPR·동의도 남기지 않는다(퇴실 뒤에는 허용하는 사후 기록이지만 취소된 접수는 다르다)

    private DischargeProgress realGuardWithCancelledReception() {
        when(receptions.findById("r-cancelled")).thenReturn(Optional.of(intake("r-cancelled", true)));
        return new DischargeProgress(
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository.class),
                receptions);
    }

    private CareServiceImpl careWith(DischargeProgress guard, kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository notes,
                                     kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository treatments,
                                     kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository mars,
                                     kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository cprs) {
        CommonCodeCache cache = new CommonCodeCache();
        return new CareServiceImpl(notes, treatments, mars, cprs, mock(TriageAssessmentRepository.class),
                mock(BedAssignmentRepository.class), receptions, cache, new CommonCodeResolver(cache),
                mock(CareMapstructMapper.class), mock(PatientClient.class), guard);
    }

    @Test
    void clinicalNotesTreatmentsMedicationsAndCprAreRejectedOnACancelledReception() {
        var notes = mock(kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository.class);
        var treatments = mock(kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository.class);
        var mars = mock(kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository.class);
        var cprs = mock(kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository.class);
        CareServiceImpl care = careWith(realGuardWithCancelledReception(), notes, treatments, mars, cprs);

        var note = new kr.co.seoulit.his.emergencyservice.care.dto.ClinicalNoteCreateRequestDto();
        note.setEncounterId("r-cancelled");
        note.setNoteTypeCode("01");
        note.setContent("c");
        note.setRecordedById("n-1");
        assertThatThrownBy(() -> care.createRecord(note)).isInstanceOf(ConflictException.class).hasMessageContaining("reception cancelled");

        var treatment = new kr.co.seoulit.his.emergencyservice.care.dto.TreatmentCreateRequestDto();
        treatment.setEncounterId("r-cancelled");
        treatment.setOrderId("o-1");
        treatment.setTreatmentCode("01");
        treatment.setPerformedById("n-1");
        assertThatThrownBy(() -> care.createTreatment(treatment)).isInstanceOf(ConflictException.class);

        var mar = new kr.co.seoulit.his.emergencyservice.care.dto.MarCreateRequestDto();
        mar.setEncounterId("r-cancelled");
        mar.setOrderId("o-1");
        mar.setAdministeredAt(LocalDateTime.now());
        mar.setDose("1");
        mar.setDrugCode("d");
        mar.setRouteCode("01");
        mar.setAdministeredById("n-1");
        assertThatThrownBy(() -> care.createMar(mar)).isInstanceOf(ConflictException.class);

        var cpr = new kr.co.seoulit.his.emergencyservice.care.dto.CprTimelineCreateRequestDto();
        cpr.setEncounterId("r-cancelled");
        cpr.setEvents(List.of(new kr.co.seoulit.his.emergencyservice.care.dto.CprTimelineCreateRequestDto.CprEventItemDto()));
        assertThatThrownBy(() -> care.createCprTimeline(cpr)).isInstanceOf(ConflictException.class);

        verify(notes, never()).save(any());
        verify(treatments, never()).save(any());
        verify(mars, never()).save(any());
        verify(cprs, never()).save(any());
    }

    @Test
    void aDischargedReceptionStillAcceptsFollowUpRecords() {
        // 퇴실(DONE)은 사후 기록을 허용한다 — 취소만 막는다
        DischargeProgress guard = realGuardWithCancelledReception();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireNotCancelled("r-ok-not-cancelled"));
    }
}
