package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreCreateRequest;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreLabItem;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCorePrescription;
import kr.co.seoulit.his.emergencyservice.order.dto.LabItemDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderVerbalConfirmRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 응급 처방 연동(검사·약품): 요청 구성·검증·전송·취소 */
class OrderServiceImplTest {

    private static final String RECEPTION_ID = "r-1";
    private static final String ORDER_ID = "3f2b8c1e-7a4d-4e5b-9c61-2d8f0a1b5e77";

    private OrderCoreClient client;
    private ReceptionIntakeRepository receptionIntakeRepository;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        client = mock(OrderCoreClient.class);
        receptionIntakeRepository = mock(ReceptionIntakeRepository.class);
        ReceptionIntake intake = new ReceptionIntake();
        intake.setId(RECEPTION_ID);
        intake.setPatientId("patient-1");
        when(receptionIntakeRepository.findById(RECEPTION_ID)).thenReturn(Optional.of(intake));
        OrderCorePrescription created = new OrderCorePrescription();
        created.setPrescriptionId(ORDER_ID);
        created.setStatus("ACTIVE");
        when(client.create(anyString(), any(OrderCoreCreateRequest.class))).thenReturn(created);
        // 전송 직후 처방코어에서 다시 읽는 실제 상태 — 기본은 검사·약제 모두 전송 완료
        when(client.get(ORDER_ID)).thenReturn(afterDispatch("SENT", "SENT"));
        service = newService(false);
    }

    /** 전송 직후의 처방: 검사 항목 하나(sendStatus)와 약제 전송 상태 */
    private OrderCorePrescription afterDispatch(String labItemStatus, String pharmacyStatus) {
        OrderItemDto labItem = lab();
        labItem.setSendStatus(labItemStatus);
        OrderCorePrescription p = new OrderCorePrescription();
        p.setPrescriptionId(ORDER_ID);
        p.setItems(List.of(labItem, drug()));
        p.setPharmacySendStatus(pharmacyStatus);
        return p;
    }

    private OrderServiceImpl newService(boolean forwardVerbalYn) {
        CommonCodeCache cache = new CommonCodeCache();
        return new OrderServiceImpl(client, receptionIntakeRepository, new CommonCodeResolver(cache), "10", forwardVerbalYn);
    }

    private OrderItemDto lab() {
        OrderItemDto item = new OrderItemDto();
        item.setPrescriptionType("검사");
        item.setItemCode("LAB001");
        item.setItemName("CBC");
        return item;
    }

    private OrderItemDto drug() {
        OrderItemDto item = new OrderItemDto();
        item.setPrescriptionType("약품");
        item.setItemCode("195700020");
        item.setItemName("타이레놀정500mg");
        item.setDosage(1.0);
        item.setDosageFormCd("TAB");
        item.setFrequency("TID");
        item.setDurationDays("3");
        return item;
    }

    private OrderCreateRequestDto request(OrderItemDto... items) {
        OrderCreateRequestDto r = new OrderCreateRequestDto();
        r.setEncounterId(RECEPTION_ID);
        r.setPrescribedBy("dr-1");
        r.setPriorityCode("01");
        r.setTimingCode("03");
        r.setItems(List.of(items));
        return r;
    }

    @Test
    void createSendsTheServerFilledFieldsAndReturnsTheOrderId() {
        OrderDto dto = service.createOrder(request(lab(), drug()));

        ArgumentCaptor<OrderCoreCreateRequest> captor = ArgumentCaptor.forClass(OrderCoreCreateRequest.class);
        verify(client).create(eq(RECEPTION_ID), captor.capture());
        OrderCoreCreateRequest body = captor.getValue();
        assertThat(body.getPatientId()).isEqualTo("patient-1");          // 접수에서 채움
        assertThat(body.getServiceType()).isEqualTo("ER");                // 고정
        assertThat(body.getDepartmentCode()).isEqualTo("10");
        assertThat(body.getOrderMethod()).isEqualTo("01");
        assertThat(body.getPrescribedBy()).isEqualTo("dr-1");
        assertThat(body.getPriorityCode()).isEqualTo("01");
        assertThat(body.getTimingCode()).isEqualTo("03");
        assertThat(body.getItems()).hasSize(2);
        // STAT 검사는 detailInfo 에 STAT, 약품은 그대로
        assertThat(body.getItems().get(0).getDetailInfo()).isEqualTo("STAT");
        assertThat(body.getItems().get(1).getDetailInfo()).isNull();
        assertThat(body.getItems().get(1).getDosage()).isEqualTo(1.0);
        assertThat(body.getVerbalYn()).isNull();

        assertThat(dto.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(dto.getEncounterId()).isEqualTo(RECEPTION_ID);
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getLabDispatchStatus()).isNull();                  // dispatchNow 가 아니면 전송하지 않는다
        verify(client, never()).dispatchLab(anyString());
    }

    @Test
    void imagingAndOtherTypesAreRejectedBecauseTheOrderCoreDoesNotTakeThem() {
        OrderItemDto imaging = lab();
        imaging.setPrescriptionType("영상");
        assertThatThrownBy(() -> service.createOrder(request(imaging)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("imaging");
        verifyNoInteractions(client);
    }

    @Test
    void invalidRequestsAreRejectedBeforeCallingTheOrderCore() {
        OrderCreateRequestDto badPriority = request(lab());
        badPriority.setPriorityCode("99");
        assertThatThrownBy(() -> service.createOrder(badPriority)).hasMessageContaining("priorityCode");

        OrderCreateRequestDto badTiming = request(lab());
        badTiming.setTimingCode("09");
        assertThatThrownBy(() -> service.createOrder(badTiming)).hasMessageContaining("timingCode");

        OrderCreateRequestDto noItems = request();
        assertThatThrownBy(() -> service.createOrder(noItems)).hasMessageContaining("items");

        OrderCreateRequestDto noDoctor = request(lab());
        noDoctor.setPrescribedBy(" ");
        assertThatThrownBy(() -> service.createOrder(noDoctor)).hasMessageContaining("prescribedBy");

        OrderItemDto noCode = lab();
        noCode.setItemCode(null);
        assertThatThrownBy(() -> service.createOrder(request(noCode))).hasMessageContaining("itemCode");

        OrderCreateRequestDto badVerbal = request(lab());
        badVerbal.setVerbalYn("X");
        assertThatThrownBy(() -> service.createOrder(badVerbal)).hasMessageContaining("verbalYn");

        verifyNoInteractions(client);
    }

    @Test
    void unknownReceptionIsNotFound() {
        OrderCreateRequestDto r = request(lab());
        r.setEncounterId("nope");
        when(receptionIntakeRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(r)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(client);
    }

    @Test
    void verbalOrderIsRegisteredWithTheVerbalMethodAndVerbalYnIsForwarded() {
        OrderServiceImpl forwarding = newService(true);
        OrderCreateRequestDto verbal = request(drug());
        verbal.setVerbalYn("Y");

        OrderDto dto = forwarding.createOrder(verbal);
        forwarding.createOrder(request(drug()));          // 구두가 아닌 일반 처방

        ArgumentCaptor<OrderCoreCreateRequest> captor = ArgumentCaptor.forClass(OrderCoreCreateRequest.class);
        verify(client, org.mockito.Mockito.times(2)).create(eq(RECEPTION_ID), captor.capture());
        OrderCoreCreateRequest verbalBody = captor.getAllValues().get(0);
        assertThat(verbalBody.getOrderMethod()).isEqualTo("02");           // 구두처방
        assertThat(verbalBody.getVerbalYn()).isEqualTo("Y");
        OrderCoreCreateRequest ordinaryBody = captor.getAllValues().get(1);
        assertThat(ordinaryBody.getOrderMethod()).isEqualTo("01");         // 전자처방
        assertThat(ordinaryBody.getVerbalYn()).isEqualTo("N");
        assertThat(dto.getVerbalYn()).isEqualTo("Y");
    }

    @Test
    void verbalYnCanBeSwitchedOffButTheVerbalMethodStillGoesOut() {
        OrderCreateRequestDto verbal = request(drug());
        verbal.setVerbalYn("Y");

        service.createOrder(verbal);                                        // setUp 의 service 는 verbalYn 전달을 끈 상태

        ArgumentCaptor<OrderCoreCreateRequest> captor = ArgumentCaptor.forClass(OrderCoreCreateRequest.class);
        verify(client).create(eq(RECEPTION_ID), captor.capture());
        assertThat(captor.getValue().getVerbalYn()).isNull();
        assertThat(captor.getValue().getOrderMethod()).isEqualTo("02");
    }

    @Test
    void confirmingAVerbalOrderCallsTheOrderCoreAndReturnsTheConfirmedState() {
        OrderCorePrescription confirmed = new OrderCorePrescription();
        confirmed.setPrescriptionId(ORDER_ID);
        confirmed.setVerbalYn("Y");
        confirmed.setVerbalConfirmedBy("dr-9");
        confirmed.setVerbalConfirmedAt("2026-10-02T12:00:00");
        confirmed.setOrderMethodName("Verbal");
        when(client.get(ORDER_ID)).thenReturn(confirmed);
        OrderVerbalConfirmRequestDto request = new OrderVerbalConfirmRequestDto();
        request.setConfirmedBy("dr-9");

        OrderDto dto = service.confirmVerbalOrder(ORDER_ID, request);

        verify(client).verbalConfirm(ORDER_ID, "dr-9");
        assertThat(dto.getVerbalConfirmedBy()).isEqualTo("dr-9");
        assertThat(dto.getVerbalConfirmedAt()).isEqualTo("2026-10-02T12:00:00");
        assertThat(dto.getOrderMethodName()).isEqualTo("Verbal");
    }

    @Test
    void confirmNeedsADoctorAndSucceedsEvenIfTheFollowUpReadFails() {
        assertThatThrownBy(() -> service.confirmVerbalOrder(ORDER_ID, new OrderVerbalConfirmRequestDto()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("confirmedBy");
        verify(client, never()).verbalConfirm(anyString(), anyString());

        when(client.get(ORDER_ID)).thenThrow(new ExternalServiceException("down"));
        OrderVerbalConfirmRequestDto request = new OrderVerbalConfirmRequestDto();
        request.setConfirmedBy("dr-9");
        OrderDto dto = service.confirmVerbalOrder(ORDER_ID, request);
        verify(client).verbalConfirm(ORDER_ID, "dr-9");
        assertThat(dto.getVerbalConfirmedBy()).isEqualTo("dr-9");
    }

    @Test
    void labItemSearchPassesTheContractFieldsThrough() {
        OrderCoreLabItem cbc = new OrderCoreLabItem();
        cbc.setItemCode("LAB001");
        cbc.setItemName("CBC");
        cbc.setTestClassification("GENERAL");
        cbc.setSpecimenTypes(List.of("BLOOD"));
        when(client.searchLabItems("cb")).thenReturn(List.of(cbc));

        List<LabItemDto> items = service.searchLabItems("cb");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemCode()).isEqualTo("LAB001");
        assertThat(items.get(0).getTestClassification()).isEqualTo("GENERAL");
        assertThat(items.get(0).getSpecimenTypes()).containsExactly("BLOOD");
    }

    @Test
    void dispatchNowSendsLabAndPharmacyOnlyForTheItemTypesPresent() {
        OrderCreateRequestDto labOnly = request(lab());
        labOnly.setDispatchNow(true);
        OrderDto dto = service.createOrder(labOnly);
        verify(client).dispatchLab(ORDER_ID);
        verify(client, never()).dispatchPharmacy(anyString());
        assertThat(dto.getLabDispatchStatus()).isEqualTo("SENT");
        assertThat(dto.getPharmacyDispatchStatus()).isEqualTo("NOT_APPLICABLE");
    }

    @Test
    void aFailedDispatchDoesNotUndoTheRegistration() {
        doThrow(new ExternalServiceException("order core is not reachable")).when(client).dispatchLab(ORDER_ID);
        OrderCreateRequestDto both = request(lab(), drug());
        both.setDispatchNow(true);

        OrderDto dto = service.createOrder(both);

        assertThat(dto.getOrderId()).isEqualTo(ORDER_ID);                 // 등록은 성공
        assertThat(dto.getLabDispatchStatus()).isEqualTo("FAILED");       // 다시 전송하도록 알린다
        assertThat(dto.getPharmacyDispatchStatus()).isEqualTo("SENT");
    }

    @Test
    void explicitDispatchReportsSentAndFailuresPropagate() {
        OrderDispatchDto lab = service.dispatchLab(ORDER_ID);
        assertThat(lab.getTarget()).isEqualTo("LAB");
        assertThat(lab.getStatus()).isEqualTo("SENT");
        assertThat(service.dispatchPharmacy(ORDER_ID).getTarget()).isEqualTo("PHARMACY");

        doThrow(new ExternalServiceException("order core error")).when(client).dispatchPharmacy("x");
        assertThatThrownBy(() -> service.dispatchPharmacy("x")).isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> service.dispatchLab(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelNeedsReasonAndUserThenReturnsTheLatestState() {
        OrderCancelRequestDto noReason = new OrderCancelRequestDto();
        noReason.setUserId("dr-1");
        assertThatThrownBy(() -> service.cancelOrder(ORDER_ID, noReason)).hasMessageContaining("cancelReason");

        OrderCorePrescription cancelled = new OrderCorePrescription();
        cancelled.setPrescriptionId(ORDER_ID);
        cancelled.setStatus("CANCELLED");
        cancelled.setCancelReason("오더 오류");
        when(client.get(ORDER_ID)).thenReturn(cancelled);
        OrderCancelRequestDto request = new OrderCancelRequestDto();
        request.setCancelReason("오더 오류");
        request.setUserId("dr-1");

        OrderDto dto = service.cancelOrder(ORDER_ID, request);

        verify(client).deactivate(ORDER_ID, "오더 오류", "dr-1");
        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelSucceedsEvenIfTheFollowUpReadFails() {
        when(client.get(ORDER_ID)).thenThrow(new ExternalServiceException("down"));
        OrderCancelRequestDto request = new OrderCancelRequestDto();
        request.setCancelReason("중복");
        request.setUserId("dr-1");

        OrderDto dto = service.cancelOrder(ORDER_ID, request);

        verify(client).deactivate(ORDER_ID, "중복", "dr-1");
        assertThat(dto.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(dto.getCancelReason()).isEqualTo("중복");
    }

    @Test
    void listReturnsTheReceptionsOrdersNewestFirstWithTheSendStatuses() {
        OrderCorePrescription older = new OrderCorePrescription();
        older.setPrescriptionId("o-old");
        older.setStatus("ORDERED");
        older.setPrescribedAt("2026-10-02T09:00:00");
        older.setLabSendStatus("SENT");
        OrderCorePrescription newer = new OrderCorePrescription();
        newer.setPrescriptionId("o-new");
        newer.setReceptionId(RECEPTION_ID);
        newer.setStatus("ORDERED");
        newer.setPrescribedAt("2026-10-02T10:30:00");
        newer.setLabSendStatus("FAILED");
        newer.setPharmacySendStatus("PENDING");
        OrderCorePrescription undated = new OrderCorePrescription();
        undated.setPrescriptionId("o-undated");
        when(client.listByReception(RECEPTION_ID)).thenReturn(List.of(older, undated, newer));

        List<OrderDto> orders = service.listOrders(RECEPTION_ID);

        assertThat(orders).extracting(OrderDto::getOrderId).containsExactly("o-new", "o-old", "o-undated");
        assertThat(orders.get(0).getLabSendStatus()).isEqualTo("FAILED");
        assertThat(orders.get(0).getPharmacySendStatus()).isEqualTo("PENDING");
        assertThat(orders.get(1).getEncounterId()).isEqualTo(RECEPTION_ID);   // 목록에 receptionId 가 없으면 요청 값으로 채움
        assertThat(orders.get(0).getItems()).isNull();                         // 가벼운 목록 — 상세는 단건 조회
    }

    @Test
    void listNeedsAnEncounterId() {
        assertThatThrownBy(() -> service.listOrders(" ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encounterId");
        verifyNoInteractions(client);
    }

    @Test
    void dispatchReportsTheRealStateEvenWhenTheOrderCoreAnswers200ButTheLabTransferFailed() {
        when(client.get(ORDER_ID)).thenReturn(afterDispatch("FAILED", "PENDING"));

        OrderDispatchDto lab = service.dispatchLab(ORDER_ID);
        OrderDispatchDto pharmacy = service.dispatchPharmacy(ORDER_ID);

        assertThat(lab.getStatus()).isEqualTo("FAILED");      // 호출은 성공했지만 검사 쪽으로 못 넘김
        assertThat(pharmacy.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void dispatchStatusIsPendingWhileSomeLabItemIsNotSentAndRequestedWhenTheStateCannotBeRead() {
        OrderItemDto sent = lab();
        sent.setSendStatus("SENT");
        OrderItemDto waiting = lab();
        waiting.setItemCode("LAB002");
        waiting.setSendStatus(null);
        OrderCorePrescription mixed = new OrderCorePrescription();
        mixed.setPrescriptionId(ORDER_ID);
        mixed.setItems(List.of(sent, waiting));
        when(client.get(ORDER_ID)).thenReturn(mixed);
        assertThat(service.dispatchLab(ORDER_ID).getStatus()).isEqualTo("PENDING");

        when(client.get(ORDER_ID)).thenThrow(new ExternalServiceException("down"));
        assertThat(service.dispatchLab(ORDER_ID).getStatus()).isEqualTo("REQUESTED");
        verify(client, org.mockito.Mockito.times(2)).dispatchLab(ORDER_ID);     // 두 번 모두 전송 호출은 했다
    }

    @Test
    void dispatchNowAtRegistrationAlsoReportsTheRealLabState() {
        when(client.get(ORDER_ID)).thenReturn(afterDispatch("FAILED", "SENT"));
        OrderCreateRequestDto both = request(lab(), drug());
        both.setDispatchNow(true);

        OrderDto dto = service.createOrder(both);

        assertThat(dto.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(dto.getLabDispatchStatus()).isEqualTo("FAILED");
        assertThat(dto.getPharmacyDispatchStatus()).isEqualTo("SENT");
    }
}
