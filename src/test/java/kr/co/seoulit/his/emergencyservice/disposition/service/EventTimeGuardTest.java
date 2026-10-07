package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 투약·CPR·동의의 사건 시각은 접수 이후, 현재 이전, (귀가·사망·자의퇴원이면) 퇴실 결정 이전이어야 한다 */
class EventTimeGuardTest {

    private static final LocalDateTime RECEIVED = LocalDateTime.now().minusHours(3);

    private DispositionRepository dispositions;
    private DischargeProgress guard;

    @BeforeEach
    void setUp() {
        ReceptionIntakeRepository receptions = mock(ReceptionIntakeRepository.class);
        dispositions = mock(DispositionRepository.class);
        ReceptionIntake intake = new ReceptionIntake();
        intake.setId("r-1");
        intake.setReceivedAt(RECEIVED);
        when(receptions.findById("r-1")).thenReturn(Optional.of(intake));
        guard = new DischargeProgress(dispositions, mock(AdmissionRequestRepository.class),
                mock(TransferNoteRepository.class), receptions);
    }

    private void decided(String typeCode, LocalDateTime decidedAt) {
        Disposition d = new Disposition();
        d.setDispositionTypeCode(typeCode);
        d.setDecidedAt(decidedAt);
        when(dispositions.findByReceptionIdOrderByDecidedAtDesc("r-1")).thenReturn(List.of(d));
    }

    @Test
    void aTimeBetweenReceptionAndNowIsAccepted() {
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", LocalDateTime.now().minusHours(1), "administeredAt"))
                .doesNotThrowAnyException();
    }

    @Test
    void aMissingTimeIsNotChecked() {
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", null, "eventAt")).doesNotThrowAnyException();
    }

    @Test
    void aTimeBeforeTheReceptionIsRejected() {
        assertThatThrownBy(() -> guard.requireEventTimeDuringStay("r-1", RECEIVED.minusMinutes(1), "administeredAt"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("administeredAt must not be before the reception time");
    }

    @Test
    void aTimeInTheFutureIsRejectedButASmallClockDifferenceIsAllowed() {
        assertThatThrownBy(() -> guard.requireEventTimeDuringStay("r-1", LocalDateTime.now().plusHours(1), "eventAt"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("eventAt must not be in the future");
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", LocalDateTime.now().plusMinutes(2), "eventAt"))
                .doesNotThrowAnyException();
    }

    @Test
    void afterAHomeDischargeATimeLaterThanTheDecisionIsRejected() {
        LocalDateTime decidedAt = LocalDateTime.now().minusHours(1);
        decided(EmgCodes.DISPOSITION_HOME, decidedAt);

        assertThatThrownBy(() -> guard.requireEventTimeDuringStay("r-1", decidedAt.plusMinutes(30), "administeredAt"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("administeredAt must not be after the discharge time");
        // 퇴실 전의 사건을 퇴실 뒤에 입력하는 것(사후 기록)은 시각이 퇴실 전이면 된다
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", decidedAt.minusMinutes(30), "administeredAt"))
                .doesNotThrowAnyException();
    }

    @Test
    void admissionAndTransferHaveNoDischargeUpperBoundBecausePatientsStayUntilTheBedOrNote() {
        LocalDateTime decidedAt = LocalDateTime.now().minusHours(1);
        decided(EmgCodes.DISPOSITION_ADMIT, decidedAt);
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", decidedAt.plusMinutes(30), "eventAt"))
                .doesNotThrowAnyException();
        decided(EmgCodes.DISPOSITION_TRANSFER, decidedAt);
        assertThatCode(() -> guard.requireEventTimeDuringStay("r-1", decidedAt.plusMinutes(30), "eventAt"))
                .doesNotThrowAnyException();
    }
}
