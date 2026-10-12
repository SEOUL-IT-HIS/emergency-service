package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 환자 목록의 입원 배지용 요약 — 최신 퇴실 결정의 유형, 입원이면 가장 최근 입원요청의 상태와 배정 병동 */
class DischargeProgressFollowUpsTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 12, 10, 0);

    private DispositionRepository dispositionRepository;
    private AdmissionRequestRepository admissionRequestRepository;
    private DischargeProgress progress;

    @BeforeEach
    void setUp() {
        dispositionRepository = mock(DispositionRepository.class);
        admissionRequestRepository = mock(AdmissionRequestRepository.class);
        progress = new DischargeProgress(dispositionRepository, admissionRequestRepository, mock(TransferNoteRepository.class),
                mock(ReceptionIntakeRepository.class));
    }

    private static Disposition disposition(String id, String receptionId, String type, LocalDateTime at) {
        Disposition d = new Disposition();
        d.setId(id);
        d.setReceptionId(receptionId);
        d.setDispositionTypeCode(type);
        d.setDecidedAt(at);
        return d;
    }

    private static AdmissionRequest request(Disposition d, String status, String ward, LocalDateTime at) {
        AdmissionRequest a = new AdmissionRequest();
        a.setDisposition(d);
        a.setRequestStatusCode(status);
        a.setAssignedWardCode(ward);
        a.setRequestedAt(at);
        return a;
    }

    @Test
    void 입원은_가장_최근_요청의_상태와_배정_병동을_알려준다() {
        Disposition waiting = disposition("d1", "r-wait", EmgCodes.DISPOSITION_ADMIT, T);
        Disposition rejectedThenAssigned = disposition("d2", "r-reassigned", EmgCodes.DISPOSITION_ADMIT, T);
        Disposition notSent = disposition("d3", "r-notsent", EmgCodes.DISPOSITION_ADMIT, T);
        when(dispositionRepository.findByReceptionIdIn(anyCollection()))
                .thenReturn(List.of(waiting, rejectedThenAssigned, notSent));
        when(admissionRequestRepository.findByDispositionIdIn(anyCollection())).thenReturn(List.of(
                request(waiting, EmgCodes.ADMISSION_REQUESTED, null, T.plusMinutes(1)),
                // 거부된 뒤 재요청해 배정된 건 — 가장 최근 요청의 상태를 본다
                request(rejectedThenAssigned, EmgCodes.ADMISSION_REJECTED, null, T.plusMinutes(1)),
                request(rejectedThenAssigned, EmgCodes.ADMISSION_BED_ASSIGNED, "02", T.plusMinutes(9))));

        Map<String, DischargeProgress.FollowUp> result = progress.followUps(List.of("r-wait", "r-reassigned", "r-notsent"));

        assertThat(result.get("r-wait")).isEqualTo(new DischargeProgress.FollowUp("02", "01", null));
        assertThat(result.get("r-reassigned")).isEqualTo(new DischargeProgress.FollowUp("02", "02", "02"));
        // 입원으로 결정했지만 요청을 아직 보내지 않음 → 상태 null
        assertThat(result.get("r-notsent")).isEqualTo(new DischargeProgress.FollowUp("02", null, null));
    }

    @Test
    void 거부된_요청은_거부로_알려준다() {
        Disposition d = disposition("d1", "r1", EmgCodes.DISPOSITION_ADMIT, T);
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(List.of(d));
        when(admissionRequestRepository.findByDispositionIdIn(anyCollection()))
                .thenReturn(List.of(request(d, EmgCodes.ADMISSION_REJECTED, null, T.plusMinutes(2))));

        assertThat(progress.followUps(List.of("r1")).get("r1").admissionStatusCode()).isEqualTo(EmgCodes.ADMISSION_REJECTED);
    }

    @Test
    void 입원이_아니면_유형만_알려주고_입원요청은_조회하지_않는다() {
        Disposition home = disposition("d1", "r-home", EmgCodes.DISPOSITION_HOME, T);
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(List.of(home));

        Map<String, DischargeProgress.FollowUp> result = progress.followUps(List.of("r-home", "r-none"));

        assertThat(result.get("r-home")).isEqualTo(new DischargeProgress.FollowUp(EmgCodes.DISPOSITION_HOME, null, null));
        // 퇴실 결정이 없는 접수는 결과에 없다
        assertThat(result).doesNotContainKey("r-none");
        org.mockito.Mockito.verifyNoInteractions(admissionRequestRepository);
    }

    @Test
    void 결정이_바뀌었으면_최신_결정_기준이다() {
        Disposition older = disposition("d-old", "r1", EmgCodes.DISPOSITION_ADMIT, T);
        Disposition newer = disposition("d-new", "r1", EmgCodes.DISPOSITION_HOME, T.plusMinutes(30));
        when(dispositionRepository.findByReceptionIdIn(anyCollection())).thenReturn(List.of(older, newer));

        assertThat(progress.followUps(List.of("r1")).get("r1"))
                .isEqualTo(new DischargeProgress.FollowUp(EmgCodes.DISPOSITION_HOME, null, null));
    }

    @Test
    void 대상이_없으면_빈_결과다() {
        assertThat(progress.followUps(List.of())).isEmpty();
    }
}
