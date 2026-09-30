package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.dto.AdmissionRequestCreateDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.AdmissionRequestDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.DispositionCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.DispositionDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.TransferNoteCreateDto;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import kr.co.seoulit.his.emergencyservice.disposition.messaging.AdmissionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 입원요청·전원 소견서: 퇴실 유형 게이트, 중복 방지, 병동 회신 상태 반영 */
class DispositionFollowUpTest {

    private DispositionRepository dispositionRepository;
    private AdmissionRequestRepository admissionRequestRepository;
    private TransferNoteRepository transferNoteRepository;
    private DispositionServiceImpl service;
    private AdmissionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        dispositionRepository = mock(DispositionRepository.class);
        admissionRequestRepository = mock(AdmissionRequestRepository.class);
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenAnswer(inv -> {
            AdmissionRequest a = inv.getArgument(0);
            a.setId("ar-new");
            return a;
        });
        when(dispositionRepository.save(any(Disposition.class))).thenAnswer(inv -> {
            Disposition d = inv.getArgument(0);
            d.setId("d-saved");
            return d;
        });
        transferNoteRepository = mock(TransferNoteRepository.class);
        CommonCodeCache cache = new CommonCodeCache();
        publisher = mock(AdmissionEventPublisher.class);
        service = new DispositionServiceImpl(dispositionRepository, admissionRequestRepository,
                transferNoteRepository, cache, new CommonCodeResolver(cache), publisher,
                new DischargeProgress(dispositionRepository, admissionRequestRepository, transferNoteRepository));
    }

    private Disposition disposition(String id, String typeCode) {
        Disposition d = new Disposition();
        d.setId(id);
        d.setDispositionTypeCode(typeCode);
        when(dispositionRepository.findById(id)).thenReturn(Optional.of(d));
        return d;
    }

    private AdmissionRequest request(Disposition d, String status) {
        AdmissionRequest a = new AdmissionRequest();
        a.setId("ar-1");
        a.setDisposition(d);
        a.setRequestStatusCode(status);
        a.setRequestedAt(LocalDateTime.now());
        return a;
    }

    @Test
    void admissionRequestNeedsAnAdmitDispositionAndStartsAsRequested() {
        disposition("d-home", EmgCodes.DISPOSITION_HOME);
        assertThatThrownBy(() -> service.createAdmissionRequest("d-home", new AdmissionRequestCreateDto()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ADMIT");

        disposition("d-admit", EmgCodes.DISPOSITION_ADMIT);
        AdmissionRequestCreateDto req = new AdmissionRequestCreateDto();
        req.setWardPrefer("06");
        req.setNote("observe for 24h");
        AdmissionRequestDto dto = service.createAdmissionRequest("d-admit", req);
        assertThat(dto.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_REQUESTED);
        // 저장 뒤 병동으로 발행(희망 병동 전달)
        org.mockito.Mockito.verify(publisher).publishRequested(any(Disposition.class), any(AdmissionRequest.class), org.mockito.ArgumentMatchers.eq("06"), org.mockito.ArgumentMatchers.eq("observe for 24h"));
    }

    @Test
    void admissionEventIsPublishedOnlyAfterTheTransactionCommits() {
        disposition("d-admit", EmgCodes.DISPOSITION_ADMIT);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.createAdmissionRequest("d-admit", new AdmissionRequestCreateDto());
            // 커밋 전에는 아직 발행하지 않는다(병동의 빠른 회신이 저장 전 요청을 못 찾는 문제 방지)
            org.mockito.Mockito.verifyNoInteractions(publisher);
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            org.mockito.Mockito.verify(publisher).publishRequested(any(Disposition.class), any(AdmissionRequest.class), any(), any());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void duplicateRequestIsBlockedUntilTheEarlierOneIsRejected() {
        Disposition d = disposition("d-admit", EmgCodes.DISPOSITION_ADMIT);
        when(admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc("d-admit"))
                .thenReturn(List.of(request(d, EmgCodes.ADMISSION_REQUESTED)));
        assertThatThrownBy(() -> service.createAdmissionRequest("d-admit", new AdmissionRequestCreateDto()))
                .isInstanceOf(ConflictException.class);

        when(admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc("d-admit"))
                .thenReturn(List.of(request(d, EmgCodes.ADMISSION_REJECTED)));
        assertThat(service.createAdmissionRequest("d-admit", new AdmissionRequestCreateDto())).isNotNull();
    }

    @Test
    void wardReplyUpdatesTheLatestRequestStatus() {
        Disposition d = disposition("d-admit", EmgCodes.DISPOSITION_ADMIT);
        AdmissionRequest latest = request(d, EmgCodes.ADMISSION_REQUESTED);
        when(admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc("d-admit"))
                .thenReturn(List.of(latest));

        AdmissionRequestDto dto = service.updateAdmissionStatus("d-admit", null, EmgCodes.ADMISSION_BED_ASSIGNED, "03");

        assertThat(dto.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_BED_ASSIGNED);
        assertThat(dto.getAssignedWardCode()).isEqualTo("03");
        assertThat(latest.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_BED_ASSIGNED);
        assertThatThrownBy(() -> service.updateAdmissionStatus("d-admit", null, "99", null))
                .hasMessageContaining("requestStatusCode");
        // 처음 회신만 반영: 뒤늦은 거부 회신이 배정 완료를 덮어쓰지 않는다
        service.updateAdmissionStatus("d-admit", null, EmgCodes.ADMISSION_REJECTED, null);
        assertThat(latest.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_BED_ASSIGNED);
    }

    @Test
    void replyIsMatchedToTheRequestItNamesEvenAfterAReRequest() {
        Disposition d = disposition("d-re", EmgCodes.DISPOSITION_ADMIT);
        AdmissionRequest oldRejected = request(d, EmgCodes.ADMISSION_REJECTED);
        oldRejected.setId("ar-old");
        AdmissionRequest fresh = request(d, EmgCodes.ADMISSION_REQUESTED);
        fresh.setId("ar-new");
        when(admissionRequestRepository.findByDispositionIdOrderByRequestedAtDesc("d-re")).thenReturn(List.of(fresh, oldRejected));

        // 옛 요청(ar-old)에 대한 늦은 회신은 새 요청을 건드리지 않는다
        service.updateAdmissionStatus("d-re", "ar-old", EmgCodes.ADMISSION_REJECTED, null);
        assertThat(fresh.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_REQUESTED);
        service.updateAdmissionStatus("d-re", "ar-new", EmgCodes.ADMISSION_BED_ASSIGNED, "03");
        assertThat(fresh.getRequestStatusCode()).isEqualTo(EmgCodes.ADMISSION_BED_ASSIGNED);
    }

    @Test
    void transferNoteNeedsATransferDispositionAndValidFields() {
        disposition("d-home", EmgCodes.DISPOSITION_HOME);
        TransferNoteCreateDto r = new TransferNoteCreateDto();
        r.setTargetHospitalCode("01");
        r.setContent("transfer summary");
        r.setWrittenById("dr-1");
        assertThatThrownBy(() -> service.createTransferNote("d-home", r))
                .hasMessageContaining("TRANSFER");

        disposition("d-tr", EmgCodes.DISPOSITION_TRANSFER);
        r.setContent(" ");
        assertThatThrownBy(() -> service.createTransferNote("d-tr", r)).hasMessageContaining("required");
        r.setContent("transfer summary");
        r.setTargetHospitalCode("99");
        assertThatThrownBy(() -> service.createTransferNote("d-tr", r)).hasMessageContaining("targetHospitalCode");
    }

    // ----- 퇴실 결정 변경 -----

    /** 접수 r-1 에 최신 결정 하나를 두고, 그 결정의 입원요청 이력을 지정한다 */
    private Disposition decided(String type, AdmissionRequest... requests) {
        Disposition d = disposition("d-old", type);
        d.setReceptionId("r-1");
        d.setDecidedAt(LocalDateTime.now().minusMinutes(10));
        when(dispositionRepository.findByReceptionIdIn(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(d));
        when(dispositionRepository.findByReceptionIdOrderByDecidedAtDesc("r-1")).thenReturn(List.of(d));
        List<AdmissionRequest> list = new java.util.ArrayList<>();
        for (AdmissionRequest a : requests) {
            a.setDisposition(d);
            list.add(a);
        }
        when(admissionRequestRepository.findByDispositionIdIn(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(list);
        return d;
    }

    private DispositionCreateRequestDto decide(String type) {
        DispositionCreateRequestDto req = new DispositionCreateRequestDto();
        req.setEncounterId("r-1");
        req.setDispositionType(type);
        return req;
    }

    @Test
    void firstDecisionIsFreeAndAdmitOrTransferStaysChangeable() {
        DispositionDto admit = service.createDisposition(decide(EmgCodes.DISPOSITION_ADMIT));
        assertThat(admit.isChangeable()).isTrue();
        DispositionDto home = service.createDisposition(decide(EmgCodes.DISPOSITION_HOME));
        assertThat(home.isChangeable()).isFalse();
    }

    @Test
    void admitCanBeChangedAfterTheWardRejectsIt() {
        decided(EmgCodes.DISPOSITION_ADMIT, request(null, EmgCodes.ADMISSION_REJECTED));
        assertThat(service.getDispositions("r-1").get(0).isChangeable()).isTrue();

        DispositionDto changed = service.createDisposition(decide(EmgCodes.DISPOSITION_TRANSFER));
        assertThat(changed.getDispositionTypeCode()).isEqualTo(EmgCodes.DISPOSITION_TRANSFER);
        // 같은 유형으로 다시 결정하는 건 의미가 없다(입원이면 재요청을 쓴다)
        assertThatThrownBy(() -> service.createDisposition(decide(EmgCodes.DISPOSITION_ADMIT)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already");
    }

    @Test
    void decisionIsLockedWhileWaitingForTheWardOrWhenFinished() {
        decided(EmgCodes.DISPOSITION_ADMIT, request(null, EmgCodes.ADMISSION_REQUESTED));
        assertThat(service.getDispositions("r-1").get(0).isChangeable()).isFalse();
        assertThatThrownBy(() -> service.createDisposition(decide(EmgCodes.DISPOSITION_HOME)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("WAITING_WARD");

        decided(EmgCodes.DISPOSITION_ADMIT, request(null, EmgCodes.ADMISSION_BED_ASSIGNED));
        assertThatThrownBy(() -> service.createDisposition(decide(EmgCodes.DISPOSITION_HOME)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("DONE");

        // 귀가 등은 결정 즉시 완료 — 중복 결정도 막힌다
        decided(EmgCodes.DISPOSITION_HOME);
        assertThatThrownBy(() -> service.createDisposition(decide(EmgCodes.DISPOSITION_HOME)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void followUpsCannotBeAttachedToAReplacedDecision() {
        Disposition old = disposition("d-admit-old", EmgCodes.DISPOSITION_ADMIT);
        old.setReceptionId("r-2");
        Disposition newer = disposition("d-home-new", EmgCodes.DISPOSITION_HOME);
        newer.setReceptionId("r-2");
        when(dispositionRepository.findByReceptionIdOrderByDecidedAtDesc("r-2")).thenReturn(List.of(newer, old));

        assertThatThrownBy(() -> service.createAdmissionRequest("d-admit-old", new AdmissionRequestCreateDto()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("replaced");
        org.mockito.Mockito.verifyNoInteractions(publisher);
    }
}
