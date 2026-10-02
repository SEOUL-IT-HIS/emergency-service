package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreCreateRequest;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCorePrescription;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 응급 처방 연동 — 검사·약품만. 영상 오더는 처방코어가 받지 않는다(2026-10-01 처방코어 회신).
 * 처방 내용은 응급 DB에 저장하지 않는다(처방코어가 원장). 응급은 처방코어가 준 prescriptionId 를 orderId 로 넘길 뿐이다.
 * 요청의 patientId·serviceType("ER")·departmentCode·orderMethod 는 응급 서버가 채우고, 우선순위·시점 코드는 호출 전에 검증한다
 * (처방코어는 잘못된 코드도 그대로 저장하므로).
 *
 * 구두처방: 지금은 일반 처방(orderMethod 01)으로 등록만 된다. 처방코어가 verbalYn·확정 API 를 내놓으면
 * app.order.forward-verbal-yn=true 로 켜고 확정 호출을 붙인다.
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    static final String TYPE_LAB = "검사";
    static final String TYPE_DRUG = "약품";
    private static final Set<String> ITEM_TYPES = Set.of(TYPE_LAB, TYPE_DRUG);

    /** 처방코어가 이 값을 ER 채널 구분에 쓴다 — 다른 값이면 외래(OPD)로 처리되므로 고정한다 */
    static final String SERVICE_TYPE_ER = "ER";
    /** ORDER_METHOD_CD 가 확정되기 전까지 처방코어가 정한 값(01 EMR) */
    static final String ORDER_METHOD_EMR = "01";

    static final String DISPATCH_SENT = "SENT";
    static final String DISPATCH_FAILED = "FAILED";
    static final String DISPATCH_NOT_APPLICABLE = "NOT_APPLICABLE";

    private final OrderCoreClient orderCoreClient;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final CommonCodeResolver codeResolver;
    private final String departmentCode;
    private final boolean forwardVerbalYn;

    public OrderServiceImpl(OrderCoreClient orderCoreClient, ReceptionIntakeRepository receptionIntakeRepository,
                            CommonCodeResolver codeResolver,
                            @Value("${app.order.department-code:10}") String departmentCode,
                            @Value("${app.order.forward-verbal-yn:false}") boolean forwardVerbalYn) {
        this.orderCoreClient = orderCoreClient;
        this.receptionIntakeRepository = receptionIntakeRepository;
        this.codeResolver = codeResolver;
        this.departmentCode = departmentCode;
        this.forwardVerbalYn = forwardVerbalYn;
    }

    @Override
    public OrderDto createOrder(OrderCreateRequestDto request) {
        validate(request);
        String receptionId = request.getEncounterId();
        ReceptionIntake reception = receptionIntakeRepository.findById(receptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("reception", receptionId));
        if (!StringUtils.hasText(reception.getPatientId())) {
            throw new IllegalArgumentException("reception has no patientId: " + receptionId);
        }

        boolean verbal = "Y".equals(request.getVerbalYn());
        OrderCoreCreateRequest body = new OrderCoreCreateRequest();
        body.setPatientId(reception.getPatientId());
        body.setPrescribedBy(request.getPrescribedBy());
        body.setDepartmentCode(departmentCode);
        body.setServiceType(SERVICE_TYPE_ER);
        body.setOrderMethod(ORDER_METHOD_EMR);
        body.setPriorityCode(request.getPriorityCode());
        body.setTimingCode(request.getTimingCode());
        if (verbal && forwardVerbalYn) {
            body.setVerbalYn("Y");
        } else if (verbal) {
            log.info("구두처방(verbalYn=Y)이지만 처방코어가 아직 지원하지 않아 일반 처방으로 등록한다 - receptionId={}", receptionId);
        }
        body.setItems(request.getItems().stream().map(item -> toCoreItem(item, request.getPriorityCode())).toList());

        OrderCorePrescription created = orderCoreClient.create(receptionId, body);
        OrderDto dto = toDto(created);
        if (dto.getEncounterId() == null) {
            dto.setEncounterId(receptionId);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            dto.setItems(request.getItems());
        }
        dto.setVerbalYn(request.getVerbalYn());

        if (request.isDispatchNow()) {
            // 등록은 이미 끝났다 — 전송이 실패해도 등록을 되돌리지 않고 상태만 알린다(전송 API 로 다시 시도)
            dto.setLabDispatchStatus(dispatchIfAny(created.getPrescriptionId(), request.getItems(), TYPE_LAB, true));
            dto.setPharmacyDispatchStatus(dispatchIfAny(created.getPrescriptionId(), request.getItems(), TYPE_DRUG, false));
        }
        log.info("응급 처방 등록 - orderId={}, receptionId={}, 항목 {}건", created.getPrescriptionId(), receptionId,
                request.getItems().size());
        return dto;
    }

    @Override
    public List<OrderDto> listOrders(String encounterId) {
        if (!StringUtils.hasText(encounterId)) {
            throw new IllegalArgumentException("encounterId is required");
        }
        // 시각은 같은 형식의 ISO 문자열이라 문자열 비교가 시간 순서와 같다. 시각이 없는 건은 뒤로.
        return orderCoreClient.listByReception(encounterId).stream()
                .map(core -> {
                    OrderDto dto = toDto(core);
                    if (dto.getEncounterId() == null) {
                        dto.setEncounterId(encounterId);
                    }
                    return dto;
                })
                .sorted(Comparator.comparing(OrderDto::getPrescribedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public OrderDto getOrder(String orderId) {
        requireOrderId(orderId);
        return toDto(orderCoreClient.get(orderId));
    }

    @Override
    public OrderDto cancelOrder(String orderId, OrderCancelRequestDto request) {
        requireOrderId(orderId);
        if (request == null || !StringUtils.hasText(request.getCancelReason()) || !StringUtils.hasText(request.getUserId())) {
            throw new IllegalArgumentException("cancelReason and userId are required");
        }
        orderCoreClient.deactivate(orderId, request.getCancelReason(), request.getUserId());
        log.info("응급 처방 취소 - orderId={}, userId={}", orderId, request.getUserId());
        try {
            return toDto(orderCoreClient.get(orderId));
        } catch (RuntimeException e) {
            // 취소는 끝났다. 최신 상태를 못 읽어도 취소 결과 자체는 성공으로 돌려준다
            log.warn("취소 뒤 처방 조회에 실패했다 - orderId={}, {}", orderId, e.getMessage());
            OrderDto dto = new OrderDto();
            dto.setOrderId(orderId);
            dto.setCancelReason(request.getCancelReason());
            return dto;
        }
    }

    @Override
    public OrderDispatchDto dispatchLab(String orderId) {
        requireOrderId(orderId);
        orderCoreClient.dispatchLab(orderId);
        return dispatched(orderId, "LAB");
    }

    @Override
    public OrderDispatchDto dispatchPharmacy(String orderId) {
        requireOrderId(orderId);
        orderCoreClient.dispatchPharmacy(orderId);
        return dispatched(orderId, "PHARMACY");
    }

    // ---------------------------------------------------------------- 내부

    private void validate(OrderCreateRequestDto request) {
        if (request == null || !StringUtils.hasText(request.getEncounterId())
                || !StringUtils.hasText(request.getPrescribedBy())) {
            throw new IllegalArgumentException("encounterId and prescribedBy are required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (!StringUtils.hasText(request.getPriorityCode()) || !StringUtils.hasText(request.getTimingCode())) {
            throw new IllegalArgumentException("priorityCode and timingCode are required");
        }
        codeResolver.require("priorityCode", request.getPriorityCode(),
                codeResolver.valueSet(EmgCodes.ORDER_PRIORITY_GROUP, EmgCodes.ORDER_PRIORITY_FALLBACK));
        codeResolver.require("timingCode", request.getTimingCode(),
                codeResolver.valueSet(EmgCodes.ORDER_TIMING_GROUP, EmgCodes.ORDER_TIMING_FALLBACK));
        if (request.getVerbalYn() == null) {
            request.setVerbalYn("N");
        }
        if (!"Y".equals(request.getVerbalYn()) && !"N".equals(request.getVerbalYn())) {
            throw new IllegalArgumentException("verbalYn must be Y or N");
        }
        for (OrderItemDto item : request.getItems()) {
            if (item == null || !ITEM_TYPES.contains(item.getPrescriptionType())) {
                throw new IllegalArgumentException("prescriptionType must be one of 검사, 약품 "
                        + "(imaging orders are not supported by the order core)");
            }
            if (!StringUtils.hasText(item.getItemCode()) || !StringUtils.hasText(item.getItemName())) {
                throw new IllegalArgumentException("itemCode and itemName are required for every item");
            }
        }
    }

    /** 요청 항목 중 해당 종류가 있으면 전송을 호출하고 결과를, 없으면 NOT_APPLICABLE 을 돌려준다. */
    private String dispatchIfAny(String orderId, List<OrderItemDto> items, String type, boolean lab) {
        if (items.stream().noneMatch(item -> type.equals(item.getPrescriptionType()))) {
            return DISPATCH_NOT_APPLICABLE;
        }
        try {
            if (lab) {
                orderCoreClient.dispatchLab(orderId);
            } else {
                orderCoreClient.dispatchPharmacy(orderId);
            }
            return DISPATCH_SENT;
        } catch (RuntimeException e) {
            log.warn("등록 직후 {} 전송 실패 - orderId={}, {}", lab ? "검사" : "약제", orderId, e.getMessage());
            return DISPATCH_FAILED;
        }
    }

    private OrderCoreCreateRequest.Item toCoreItem(OrderItemDto item, String priorityCode) {
        OrderCoreCreateRequest.Item core = new OrderCoreCreateRequest.Item();
        core.setPrescriptionType(item.getPrescriptionType());
        core.setItemCode(item.getItemCode());
        core.setItemName(item.getItemName());
        core.setDosage(item.getDosage());
        core.setDosageFormCd(item.getDosageFormCd());
        core.setFrequency(item.getFrequency());
        core.setDurationDays(item.getDurationDays());
        // 처방코어 예시처럼 STAT 검사는 detailInfo 에 STAT 를 적는다(비워 둔 경우만)
        boolean statLab = TYPE_LAB.equals(item.getPrescriptionType())
                && EmgCodes.ORDER_PRIORITY_STAT.equals(priorityCode) && !StringUtils.hasText(item.getDetailInfo());
        core.setDetailInfo(statLab ? "STAT" : item.getDetailInfo());
        return core;
    }

    private OrderDto toDto(OrderCorePrescription core) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(core.getPrescriptionId());
        dto.setEncounterId(core.getReceptionId());
        dto.setStatus(core.getStatus());
        dto.setOrderMethod(core.getOrderMethod());
        dto.setPriorityCode(core.getPriorityCode());
        dto.setTimingCode(core.getTimingCode());
        dto.setVerbalYn(core.getVerbalYn());
        dto.setPrescribedBy(core.getPrescribedBy());
        dto.setPrescribedAt(core.getPrescribedAt());
        dto.setCancelledAt(core.getCancelledAt());
        dto.setCancelReason(core.getCancelReason());
        dto.setItems(core.getItems());
        dto.setLabSendStatus(core.getLabSendStatus());
        dto.setPharmacySendStatus(core.getPharmacySendStatus());
        return dto;
    }

    private OrderDispatchDto dispatched(String orderId, String target) {
        OrderDispatchDto dto = new OrderDispatchDto();
        dto.setOrderId(orderId);
        dto.setTarget(target);
        dto.setStatus(DISPATCH_SENT);
        return dto;
    }

    private void requireOrderId(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("orderId is required");
        }
    }
}
