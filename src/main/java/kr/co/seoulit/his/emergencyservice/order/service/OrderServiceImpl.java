package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreCreateRequest;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCorePrescription;
import kr.co.seoulit.his.emergencyservice.order.dto.LabItemDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderVerbalConfirmRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 응급 처방 연동 — 검사·약품만. 영상 오더는 처방코어가 받지 않는다(2026-10-01 처방코어 회신).
 * 처방 내용은 응급 DB에 저장하지 않는다(처방코어가 원장). 응급은 처방코어가 준 prescriptionId 를 orderId 로 넘길 뿐이다.
 * 요청의 patientId·serviceType("ER")·departmentCode·orderMethod 는 응급 서버가 채우고, 우선순위·시점 코드는 호출 전에 검증한다
 * (처방코어는 잘못된 코드도 그대로 저장하므로).
 *
 * 구두처방: verbalYn=Y 이면 orderMethod 02(구두)로 등록하고 verbalYn 도 처방코어로 전달한다. 사후 확정은
 * PATCH …/verbal-confirm?confirmedBy= (확정 일시·확정 의사 기록). app.order.forward-verbal-yn 으로 verbalYn 전달을 끌 수 있다.
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    static final String TYPE_LAB = "검사";
    static final String TYPE_DRUG = "약품";
    private static final Set<String> ITEM_TYPES = Set.of(TYPE_LAB, TYPE_DRUG);

    /** 처방코어가 이 값을 ER 채널 구분에 쓴다 — 다른 값이면 외래(OPD)로 처리되므로 고정한다 */
    static final String SERVICE_TYPE_ER = "ER";
    /** 처방방법 코드(ADM ORDER_METHOD_CD, 2026-10-02 확정): 01 전자처방 / 02 구두처방 / 03 전화처방 */
    static final String ORDER_METHOD_ELECTRONIC = "01";
    static final String ORDER_METHOD_VERBAL = "02";

    static final String DISPATCH_SENT = "SENT";
    static final String DISPATCH_FAILED = "FAILED";
    static final String DISPATCH_PENDING = "PENDING";
    static final String DISPATCH_NOT_APPLICABLE = "NOT_APPLICABLE";
    /** 전송을 요청했지만 그 뒤 실제 상태를 읽지 못했다 */
    static final String DISPATCH_REQUESTED = "REQUESTED";

    private final OrderCoreClient orderCoreClient;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final CommonCodeResolver codeResolver;
    private final DischargeProgress dischargeProgress;
    private final String departmentCode;
    private final boolean forwardVerbalYn;
    private final Duration labItemCacheTtl;
    /** 마지막으로 처방코어에서 받은 검사항목 전체 목록 — 처방코어↔LAB 연결이 잠깐 끊겨도 화면이 버티게 한다 */
    private volatile CachedLabItems labItemCache;

    private record CachedLabItems(List<LabItemDto> items, Instant loadedAt) {
    }

    public OrderServiceImpl(OrderCoreClient orderCoreClient, ReceptionIntakeRepository receptionIntakeRepository,
                            CommonCodeResolver codeResolver, DischargeProgress dischargeProgress,
                            @Value("${app.order.department-code:10}") String departmentCode,
                            @Value("${app.order.forward-verbal-yn:true}") boolean forwardVerbalYn,
                            @Value("${app.order.lab-item-cache-minutes:10}") long labItemCacheMinutes) {
        this.orderCoreClient = orderCoreClient;
        this.receptionIntakeRepository = receptionIntakeRepository;
        this.codeResolver = codeResolver;
        this.dischargeProgress = dischargeProgress;
        this.departmentCode = departmentCode;
        this.forwardVerbalYn = forwardVerbalYn;
        this.labItemCacheTtl = Duration.ofMinutes(Math.max(labItemCacheMinutes, 0));
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
        // 퇴실이 끝난 환자에게는 새 처방을 내지 않는다(기존 처방의 취소·전송·구두 확정은 정리 작업이라 허용)
        dischargeProgress.requireNotDischarged(receptionId);

        boolean verbal = "Y".equals(request.getVerbalYn());
        OrderCoreCreateRequest body = new OrderCoreCreateRequest();
        body.setPatientId(reception.getPatientId());
        body.setPrescribedBy(request.getPrescribedBy());
        body.setDepartmentCode(departmentCode);
        body.setServiceType(SERVICE_TYPE_ER);
        body.setOrderMethod(verbal ? ORDER_METHOD_VERBAL : ORDER_METHOD_ELECTRONIC);
        body.setPriorityCode(request.getPriorityCode());
        body.setTimingCode(request.getTimingCode());
        if (forwardVerbalYn) {
            body.setVerbalYn(verbal ? "Y" : "N");
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

    /**
     * 검사항목은 목록이 작고 거의 바뀌지 않아 전체 목록을 받아 두고(기본 10분) 이름·코드는 응급에서 걸러서 돌려준다.
     * 처방코어가 LAB 연결 불안정으로 검색에 실패해도(502), 받아 둔 목록이 있으면 오래됐어도 그걸로 응답한다 — 없을 때만 오류.
     */
    @Override
    public List<LabItemDto> searchLabItems(String name) {
        CachedLabItems cached = labItemCache;
        if (cached != null && !labItemCacheTtl.isZero()
                && Duration.between(cached.loadedAt(), Instant.now()).compareTo(labItemCacheTtl) < 0) {
            return filterLabItems(cached.items(), name);
        }
        try {
            List<LabItemDto> all = orderCoreClient.searchLabItems(null).stream().map(core -> {
                LabItemDto dto = new LabItemDto();
                dto.setItemCode(core.getItemCode());
                dto.setItemName(core.getItemName());
                dto.setTestClassification(core.getTestClassification());
                dto.setSpecimenTypes(core.getSpecimenTypes());
                return dto;
            }).toList();
            labItemCache = new CachedLabItems(all, Instant.now());
            return filterLabItems(all, name);
        } catch (ExternalServiceException e) {
            if (cached == null) {
                throw e;
            }
            log.warn("검사항목 검색이 실패해 마지막으로 받아 둔 목록으로 응답한다 - {}", e.getMessage());
            return filterLabItems(cached.items(), name);
        }
    }

    /** 이름·코드에 입력한 글자가 들어 있는 항목(대소문자 구분 없음). 입력이 없으면 전체 */
    private List<LabItemDto> filterLabItems(List<LabItemDto> items, String name) {
        if (!StringUtils.hasText(name)) {
            return items;
        }
        String needle = name.trim().toLowerCase();
        return items.stream()
                .filter(item -> (item.getItemName() != null && item.getItemName().toLowerCase().contains(needle))
                        || (item.getItemCode() != null && item.getItemCode().toLowerCase().contains(needle)))
                .toList();
    }

    @Override
    public OrderDto confirmVerbalOrder(String orderId, OrderVerbalConfirmRequestDto request) {
        requireOrderId(orderId);
        if (request == null || !StringUtils.hasText(request.getConfirmedBy())) {
            throw new IllegalArgumentException("confirmedBy is required");
        }
        orderCoreClient.verbalConfirm(orderId, request.getConfirmedBy());
        log.info("구두처방 확정 - orderId={}, confirmedBy={}", orderId, request.getConfirmedBy());
        try {
            return toDto(orderCoreClient.get(orderId));
        } catch (RuntimeException e) {
            // 확정은 끝났다. 최신 상태를 못 읽어도 확정 결과 자체는 성공으로 돌려준다
            log.warn("구두 확정 뒤 처방 조회에 실패했다 - orderId={}, {}", orderId, e.getMessage());
            OrderDto dto = new OrderDto();
            dto.setOrderId(orderId);
            dto.setVerbalYn("Y");
            dto.setVerbalConfirmedBy(request.getConfirmedBy());
            return dto;
        }
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
        return dispatched(orderId, "LAB", sendStatusAfterDispatch(orderId, true));
    }

    @Override
    public OrderDispatchDto dispatchPharmacy(String orderId) {
        requireOrderId(orderId);
        orderCoreClient.dispatchPharmacy(orderId);
        return dispatched(orderId, "PHARMACY", sendStatusAfterDispatch(orderId, false));
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
            return sendStatusAfterDispatch(orderId, lab);
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
        dto.setOrderMethodName(core.getOrderMethodName());
        dto.setVerbalConfirmedAt(core.getVerbalConfirmedAt());
        dto.setVerbalConfirmedBy(core.getVerbalConfirmedBy());
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

    private OrderDispatchDto dispatched(String orderId, String target, String status) {
        OrderDispatchDto dto = new OrderDispatchDto();
        dto.setOrderId(orderId);
        dto.setTarget(target);
        dto.setStatus(status);
        return dto;
    }

    /**
     * 전송 호출이 성공(HTTP 2xx)해도 처방코어가 검사·약제 쪽으로 실제로 넘겼는지는 별개다 — 실서버 확인에서
     * dispatch-lab 이 200 을 주고도 검사 항목이 FAILED 로 남는 경우가 있었다. 그래서 호출 직후 처방을 다시 읽어
     * 실제 전송 상태를 돌려준다(읽지 못하면 REQUESTED).
     */
    private String sendStatusAfterDispatch(String orderId, boolean lab) {
        try {
            OrderCorePrescription current = orderCoreClient.get(orderId);
            String status = lab ? labSendStatusOf(current) : current.getPharmacySendStatus();
            return StringUtils.hasText(status) ? status : DISPATCH_REQUESTED;
        } catch (RuntimeException e) {
            log.warn("전송 뒤 처방 상태를 읽지 못했다 - orderId={}, {}", orderId, e.getMessage());
            return DISPATCH_REQUESTED;
        }
    }

    /**
     * 검사 항목 전송 상태 요약: 하나라도 실제로 실패했으면 FAILED, 전부 LAB 이 받았으면 SENT, 그 밖에는 PENDING, 검사 항목이 없으면 null.
     * LAB 이 이미 받은 항목은 sendStatus 가 FAILED 로 남아 있어도 SENT 로 본다 — 같은 처방을 다시 전송하면 LAB 이
     * "이미 접수된 오더입니다"로 거절하면서 처방코어가 항목을 FAILED 로 덮어쓰기 때문이다(실서버에서 확인).
     */
    static String labSendStatusOf(OrderCorePrescription prescription) {
        List<OrderItemDto> labItems = prescription.getItems() == null ? List.of()
                : prescription.getItems().stream().filter(item -> TYPE_LAB.equals(item.getPrescriptionType())).toList();
        if (labItems.isEmpty()) {
            return null;
        }
        if (labItems.stream().anyMatch(item -> DISPATCH_FAILED.equals(item.getSendStatus()) && !receivedByLab(item))) {
            return DISPATCH_FAILED;
        }
        return labItems.stream().allMatch(item -> DISPATCH_SENT.equals(item.getSendStatus()) || receivedByLab(item))
                ? DISPATCH_SENT : DISPATCH_PENDING;
    }

    /** LAB 이 이 항목을 이미 받았는지: LAB 오더번호가 있거나, 거절 사유가 "이미 접수된 오더"(중복 전송)이다 */
    static boolean receivedByLab(OrderItemDto item) {
        return StringUtils.hasText(item.getLabOrderId())
                || (item.getRejectReason() != null && item.getRejectReason().contains("이미 접수"));
    }

    private void requireOrderId(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("orderId is required");
        }
    }
}
