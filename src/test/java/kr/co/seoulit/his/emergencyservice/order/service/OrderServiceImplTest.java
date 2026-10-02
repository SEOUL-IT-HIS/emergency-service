package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreCreateRequest;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCorePrescription;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
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
        service = newService(false);
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
    void verbalOrderIsRegisteredAsAnOrdinaryOrderUntilTheOrderCoreSupportsIt() {
        OrderCreateRequestDto verbal = request(drug());
        verbal.setVerbalYn("Y");

        OrderDto dto = service.createOrder(verbal);

        ArgumentCaptor<OrderCoreCreateRequest> captor = ArgumentCaptor.forClass(OrderCoreCreateRequest.class);
        verify(client).create(eq(RECEPTION_ID), captor.capture());
        assertThat(captor.getValue().getVerbalYn()).isNull();             // 아직 전달하지 않는다
        assertThat(captor.getValue().getOrderMethod()).isEqualTo("01");
        assertThat(dto.getVerbalYn()).isEqualTo("Y");                     // 요청 표시는 응답에 남는다

        // 처방코어가 지원하면 설정으로 켠다
        OrderServiceImpl forwarding = newService(true);
        forwarding.createOrder(verbal);
        ArgumentCaptor<OrderCoreCreateRequest> second = ArgumentCaptor.forClass(OrderCoreCreateRequest.class);
        verify(client, org.mockito.Mockito.times(2)).create(eq(RECEPTION_ID), second.capture());
        assertThat(second.getAllValues().get(1).getVerbalYn()).isEqualTo("Y");
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
}
