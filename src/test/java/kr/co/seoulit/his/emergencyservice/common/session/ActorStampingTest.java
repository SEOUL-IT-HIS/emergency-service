package kr.co.seoulit.his.emergencyservice.common.session;

import kr.co.seoulit.his.common.session.SessionUser;
import kr.co.seoulit.his.emergencyservice.care.controller.CareController;
import kr.co.seoulit.his.emergencyservice.care.controller.ConsentController;
import kr.co.seoulit.his.emergencyservice.care.dto.ClinicalNoteCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.CprTimelineCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.MarCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.TreatmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import kr.co.seoulit.his.emergencyservice.care.service.ConsentService;
import kr.co.seoulit.his.emergencyservice.disposition.controller.DispositionController;
import kr.co.seoulit.his.emergencyservice.disposition.dto.DispositionCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.TransferNoteCreateDto;
import kr.co.seoulit.his.emergencyservice.disposition.service.DispositionService;
import kr.co.seoulit.his.emergencyservice.monitor.controller.MonitorController;
import kr.co.seoulit.his.emergencyservice.monitor.dto.LosAlertAcknowledgeRequestDto;
import kr.co.seoulit.his.emergencyservice.monitor.service.MonitorService;
import kr.co.seoulit.his.emergencyservice.order.controller.OrderController;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderVerbalConfirmRequestDto;
import kr.co.seoulit.his.emergencyservice.order.service.OrderService;
import kr.co.seoulit.his.emergencyservice.resource.controller.ResourceController;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedAssignmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedReleaseRequestDto;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import kr.co.seoulit.his.emergencyservice.triage.controller.TriageController;
import kr.co.seoulit.his.emergencyservice.triage.dto.IsolationCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.KtasCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.KtasUpdateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.RiskScreeningCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.dto.VitalAssessmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.triage.service.TriageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 사용자가 처리자인 칸은 화면이 보낸 값이 아니라 세션의 empId 로 기록된다.
 * 의사를 골라 보내는 칸(처방의·구두 확정 의사·퇴실/격리 결정자·전원소견서 작성자)은 보낸 값이 그대로 간다.
 */
class ActorStampingTest {

    private final LoginUserResolver resolver = new LoginUserResolver();

    private static MockHttpSession loggedIn() {
        SessionUser user = new SessionUser();
        user.setEmpId("EMP-ME");
        user.setEmpName("me");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        return session;
    }

    private static void send(Object controller, MockHttpServletRequestBuilder request, String json, boolean login) throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        request.contentType(MediaType.APPLICATION_JSON).content(json);
        if (login) {
            request.session(loggedIn());
        }
        mvc.perform(request).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- 로그인 사용자가 처리자인 칸

    @Test
    void clinicalNoteRecorder() throws Exception {
        CareService service = mock(CareService.class);
        send(new CareController(service, resolver), post("/api/emergency/care/records"),
                "{\"encounterId\":\"r1\",\"noteTypeCode\":\"01\",\"content\":\"x\",\"recordedById\":\"spoofed\"}", true);

        ArgumentCaptor<ClinicalNoteCreateRequestDto> captor = ArgumentCaptor.forClass(ClinicalNoteCreateRequestDto.class);
        verify(service).createRecord(captor.capture());
        assertThat(captor.getValue().getRecordedById()).isEqualTo("EMP-ME");
    }

    @Test
    void treatmentPerformer() throws Exception {
        CareService service = mock(CareService.class);
        send(new CareController(service, resolver), post("/api/emergency/care/treatments"),
                "{\"encounterId\":\"r1\",\"orderId\":\"o1\",\"treatmentCode\":\"01\",\"performedById\":\"spoofed\"}", true);

        ArgumentCaptor<TreatmentCreateRequestDto> captor = ArgumentCaptor.forClass(TreatmentCreateRequestDto.class);
        verify(service).createTreatment(captor.capture());
        assertThat(captor.getValue().getPerformedById()).isEqualTo("EMP-ME");
    }

    @Test
    void medicationAdministrator() throws Exception {
        CareService service = mock(CareService.class);
        send(new CareController(service, resolver), post("/api/emergency/care/medication-administrations"),
                "{\"encounterId\":\"r1\",\"orderId\":\"o1\",\"drugCode\":\"d\",\"administeredById\":\"spoofed\"}", true);

        ArgumentCaptor<MarCreateRequestDto> captor = ArgumentCaptor.forClass(MarCreateRequestDto.class);
        verify(service).createMar(captor.capture());
        assertThat(captor.getValue().getAdministeredById()).isEqualTo("EMP-ME");
    }

    @Test
    void everyCprEventRecorder() throws Exception {
        CareService service = mock(CareService.class);
        send(new CareController(service, resolver), post("/api/emergency/care/cpr-timelines"),
                "{\"encounterId\":\"r1\",\"events\":[{\"eventTypeCode\":\"01\",\"recordedById\":\"a\"},{\"eventTypeCode\":\"02\"}]}", true);

        ArgumentCaptor<CprTimelineCreateRequestDto> captor = ArgumentCaptor.forClass(CprTimelineCreateRequestDto.class);
        verify(service).createCprTimeline(captor.capture());
        assertThat(captor.getValue().getEvents()).extracting(CprTimelineCreateRequestDto.CprEventItemDto::getRecordedById)
                .containsExactly("EMP-ME", "EMP-ME");
    }

    @Test
    void consentRecorder() throws Exception {
        ConsentService service = mock(ConsentService.class);
        send(new ConsentController(service, resolver), post("/api/emergency/care/consents"),
                "{\"encounterId\":\"r1\",\"recordedById\":\"spoofed\"}", true);

        ArgumentCaptor<ConsentRecordCreateRequestDto> captor = ArgumentCaptor.forClass(ConsentRecordCreateRequestDto.class);
        verify(service).createConsent(captor.capture());
        assertThat(captor.getValue().getRecordedById()).isEqualTo("EMP-ME");
    }

    @Test
    void ktasAssessorOnCreateAndReassess() throws Exception {
        TriageService service = mock(TriageService.class);
        send(new TriageController(service, resolver), post("/api/emergency/triage/ktas"),
                "{\"encounterId\":\"r1\",\"ktasScore\":\"01\",\"assessedById\":\"spoofed\"}", true);
        send(new TriageController(service, resolver), put("/api/emergency/triage/ktas/k1"),
                "{\"ktasScore\":\"02\",\"assessedById\":\"spoofed\"}", true);

        ArgumentCaptor<KtasCreateRequestDto> created = ArgumentCaptor.forClass(KtasCreateRequestDto.class);
        verify(service).createKtas(created.capture());
        assertThat(created.getValue().getAssessedById()).isEqualTo("EMP-ME");
        ArgumentCaptor<KtasUpdateRequestDto> updated = ArgumentCaptor.forClass(KtasUpdateRequestDto.class);
        verify(service).updateKtas(eq("k1"), updated.capture());
        assertThat(updated.getValue().getAssessedById()).isEqualTo("EMP-ME");
    }

    @Test
    void vitalsMeasurer() throws Exception {
        TriageService service = mock(TriageService.class);
        send(new TriageController(service, resolver), post("/api/emergency/triage/vital-assessments"),
                "{\"encounterId\":\"r1\",\"vitals\":[{\"heartRate\":80}]}", true);

        ArgumentCaptor<VitalAssessmentCreateRequestDto> captor = ArgumentCaptor.forClass(VitalAssessmentCreateRequestDto.class);
        verify(service).createVitalAssessments(captor.capture());
        assertThat(captor.getValue().getMeasuredById()).as("보내지 않아도 로그인 사용자로 채워진다").isEqualTo("EMP-ME");
    }

    @Test
    void riskScreeningScreener() throws Exception {
        TriageService service = mock(TriageService.class);
        send(new TriageController(service, resolver), post("/api/emergency/triage/risk-screenings"),
                "{\"encounterId\":\"r1\",\"screenType\":\"SEPSIS\",\"screenedById\":\"spoofed\"}", true);

        ArgumentCaptor<RiskScreeningCreateRequestDto> captor = ArgumentCaptor.forClass(RiskScreeningCreateRequestDto.class);
        verify(service).createRiskScreening(captor.capture());
        assertThat(captor.getValue().getScreenedById()).isEqualTo("EMP-ME");
    }

    @Test
    void bedAssignerAndReleaser() throws Exception {
        ResourceService service = mock(ResourceService.class);
        send(new ResourceController(service, resolver), post("/api/emergency/resources/bed-assignments"),
                "{\"encounterId\":\"r1\",\"bedId\":\"b1\",\"assignedById\":\"spoofed\"}", true);
        send(new ResourceController(service, resolver), patch("/api/emergency/resources/bed-assignments/a1/release"),
                "{\"releasedById\":\"spoofed\"}", true);

        ArgumentCaptor<BedAssignmentCreateRequestDto> assigned = ArgumentCaptor.forClass(BedAssignmentCreateRequestDto.class);
        verify(service).assignBed(assigned.capture());
        assertThat(assigned.getValue().getAssignedById()).isEqualTo("EMP-ME");
        ArgumentCaptor<BedReleaseRequestDto> released = ArgumentCaptor.forClass(BedReleaseRequestDto.class);
        verify(service).releaseBed(eq("a1"), released.capture());
        assertThat(released.getValue().getReleasedById()).isEqualTo("EMP-ME");
    }

    @Test
    void longStayAlertAcknowledger() throws Exception {
        MonitorService service = mock(MonitorService.class);
        send(new MonitorController(service, resolver), patch("/api/emergency/monitor/long-stay-alerts/l1/acknowledge"),
                "{\"acknowledgedById\":\"spoofed\"}", true);

        ArgumentCaptor<LosAlertAcknowledgeRequestDto> captor = ArgumentCaptor.forClass(LosAlertAcknowledgeRequestDto.class);
        verify(service).acknowledgeLongStayAlert(eq("l1"), captor.capture());
        assertThat(captor.getValue().getAcknowledgedById()).isEqualTo("EMP-ME");
    }

    @Test
    void orderCanceller() throws Exception {
        OrderService service = mock(OrderService.class);
        send(new OrderController(service, resolver), patch("/api/emergency/orders/o1/cancel"),
                "{\"cancelReason\":\"wrong\",\"userId\":\"spoofed\"}", true);

        ArgumentCaptor<OrderCancelRequestDto> captor = ArgumentCaptor.forClass(OrderCancelRequestDto.class);
        verify(service).cancelOrder(eq("o1"), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("EMP-ME");
    }

    // ---------------------------------------------------------------- 의사를 골라 보내는 칸은 그대로

    @Test
    void doctorFieldsKeepTheChosenDoctorEvenWhenSomeoneElseIsLoggedIn() throws Exception {
        // 구두처방은 간호사가 로그인해 의사 대신 입력하는 경우가 많다
        OrderService orders = mock(OrderService.class);
        send(new OrderController(orders, resolver), post("/api/emergency/orders"),
                "{\"encounterId\":\"r1\",\"prescribedBy\":\"DR-CHOSEN\",\"items\":[]}", true);
        send(new OrderController(orders, resolver), patch("/api/emergency/orders/o1/verbal-confirm"),
                "{\"confirmedBy\":\"DR-CHOSEN\"}", true);
        ArgumentCaptor<OrderCreateRequestDto> created = ArgumentCaptor.forClass(OrderCreateRequestDto.class);
        verify(orders).createOrder(created.capture());
        assertThat(created.getValue().getPrescribedBy()).isEqualTo("DR-CHOSEN");
        ArgumentCaptor<OrderVerbalConfirmRequestDto> confirmed = ArgumentCaptor.forClass(OrderVerbalConfirmRequestDto.class);
        verify(orders).confirmVerbalOrder(eq("o1"), confirmed.capture());
        assertThat(confirmed.getValue().getConfirmedBy()).isEqualTo("DR-CHOSEN");

        DispositionService dispositions = mock(DispositionService.class);
        send(new DispositionController(dispositions), post("/api/emergency/dispositions"),
                "{\"encounterId\":\"r1\",\"dispositionType\":\"01\",\"decidedById\":\"DR-CHOSEN\"}", true);
        send(new DispositionController(dispositions), post("/api/emergency/dispositions/d1/transfer-note"),
                "{\"targetHospitalCode\":\"01\",\"content\":\"x\",\"writtenById\":\"DR-CHOSEN\"}", true);
        ArgumentCaptor<DispositionCreateRequestDto> decided = ArgumentCaptor.forClass(DispositionCreateRequestDto.class);
        verify(dispositions).createDisposition(decided.capture());
        assertThat(decided.getValue().getDecidedById()).isEqualTo("DR-CHOSEN");
        ArgumentCaptor<TransferNoteCreateDto> written = ArgumentCaptor.forClass(TransferNoteCreateDto.class);
        verify(dispositions).createTransferNote(eq("d1"), written.capture());
        assertThat(written.getValue().getWrittenById()).isEqualTo("DR-CHOSEN");

        TriageService triage = mock(TriageService.class);
        send(new TriageController(triage, resolver), post("/api/emergency/triage/infection-isolations"),
                "{\"encounterId\":\"r1\",\"isolationTypeCode\":\"01\",\"decidedById\":\"DR-CHOSEN\"}", true);
        ArgumentCaptor<IsolationCreateRequestDto> isolated = ArgumentCaptor.forClass(IsolationCreateRequestDto.class);
        verify(triage).createIsolation(isolated.capture());
        assertThat(isolated.getValue().getDecidedById()).isEqualTo("DR-CHOSEN");
    }

    // ---------------------------------------------------------------- 로그인 정보가 없는 환경

    @Test
    void withoutALoginTheTypedValueIsKept() throws Exception {
        CareService service = mock(CareService.class);
        send(new CareController(service, resolver), post("/api/emergency/care/records"),
                "{\"encounterId\":\"r1\",\"noteTypeCode\":\"01\",\"content\":\"x\",\"recordedById\":\"typed-id\"}", false);

        ArgumentCaptor<ClinicalNoteCreateRequestDto> captor = ArgumentCaptor.forClass(ClinicalNoteCreateRequestDto.class);
        verify(service).createRecord(captor.capture());
        assertThat(captor.getValue().getRecordedById()).isEqualTo("typed-id");
    }
}
