# API 명세

근거: `응급관리_API_목록(응급전용제외).xlsx` (2026-07-16), `전체서비스_API카탈로그.xlsx`  
Base path (EMG Provider): `/api/emergency`  
공통 응답: `{ "code", "message", "data" }`  
Swagger UI: `http://localhost:8085/swagger-ui.html` (코드 기반 자동 생성 명세 — 상세 스키마는 Swagger 참조)

---

## 1. 역할 구분

| 구분 | 설명 |
| --- | --- |
| **EMG Provider** | emergency-service가 구현·소유 |
| **GR2 Consumer** | 프론트/BFF가 GR2 `/api/orders*` 호출 (EMG는 UI·연동) |
| **타서비스 Consumer** | PAT / LAB / PHM / ADM / RCP 등 조회 |

---

## 2. EMG Provider API

### 2.1 ER-TRIAGE

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-TRI-01 | GET | `/api/emergency/triage/ems-info` | 119 사전정보 | - |
| UC-TRI-02 | POST | `/api/emergency/triage/ktas` | KTAS 최초 분류 | patientId, encounterId, ktasScore |
| UC-TRI-03 | PUT | `/api/emergency/triage/ktas/{id}` | KTAS 재평가 | id |
| UC-TRI-04 | POST | `/api/emergency/triage/vital-assessments` | 활력징후 | encounterId, vitals[] |
| UC-TRI-05 | POST | `/api/emergency/triage/infection-isolations` | 격리 등록 | patientId (DUR은 GR2/PHM 조회) |
| UC-TRI-06 | POST | `/api/emergency/triage/risk-screenings` | 패혈증/뇌졸중 | encounterId, screenType |

### 2.2 ER-RESOURCE

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-RES-01 | GET | `/api/emergency/resources/congestion` | 혼잡도/과밀화 | - |
| UC-RES-02 | POST | `/api/emergency/resources/bed-assignments` | 병상 배정 | encounterId, bedId |
| UC-RES-03 | POST | `/api/emergency/resources/equipment-assignments` | 기기 할당 | encounterId, equipmentId |

### 2.3 ER-CARE

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-CARE-01 | GET | `/api/emergency/care/patients` | 응급환자 목록 | date, status? |
| UC-CARE-02 | POST | `/api/emergency/care/records` | 진료기록 | encounterId, content |
| UC-CARE-03 | POST | `/api/emergency/care/treatments` | 처치기록 | encounterId, treatmentCode, orderId?(GR2) |
| UC-CARE-04 | POST | `/api/emergency/care/medication-administrations` | MAR | encounterId, **orderId(GR2)**, administeredAt, dose |
| UC-CARE-05 | POST | `/api/emergency/care/cpr-timelines` | CPR | encounterId, events[] |

### 2.4 ER-CHANNEL

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-ORD-09 | POST | `/api/emergency/consultations` | 협진 (오더 원장 아님) | encounterId, specialty, reason |
| UC-ORD-11 | POST | `/api/emergency/on-call-pages` | 당직 호출 | encounterId, targetRole |

### 2.5 ER-MONITOR

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-MON-01 | GET | `/api/emergency/monitor/dashboard` | 종합 현황판 | - |
| UC-MON-02 | GET | `/api/emergency/monitor/long-stay-alerts` | 장기체류 알림 | thresholdHours? |
| UC-MON-03 | GET | `/api/emergency/monitor/external-hospitals` | 외부병원/NEDIS | - |

### 2.6 ER-DISPOSITION

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-DISP-01 | POST | `/api/emergency/dispositions` | 퇴실 결정 | encounterId, dispositionType |
| UC-DISP-02 | POST | `/api/emergency/dispositions/{id}/admission-request` | 입원 요청 | id, wardPrefer? |
| UC-DISP-03 | POST | `/api/emergency/dispositions/{id}/transfer-note` | 전원 소견서 | id (투약은 GR2 조회) |
| UC-DISP-04 | POST | `/api/emergency/dispositions/{id}/ambulance-transport` | 이송 기록 | id, transportMeta |

### 2.7 ER-CODE (응급 전용)

| No | Method | Endpoint | 설명 |
| --- | --- | --- | --- |
| 1 | GET | `/api/emergency/codes` | 코드그룹·코드 목록 |
| 2 | GET | `/api/emergency/codes/{groupCode}` | 그룹별 코드 |
| 3 | POST | `/api/emergency/codes` | 등록 |
| 4 | PUT | `/api/emergency/codes/{codeId}` | 수정 (코드값 변경 지양) |
| 5 | PUT | `/api/emergency/codes/{codeId}/use-yn` | 사용여부 (삭제 대체) |

---

## 3. GR2 처방코어 Consumer 호출 (EMG UI)

`encounterType=ER` 고정. Provider = GR2.

| UC | Method | Endpoint | 설명 | 필수 |
| --- | --- | --- | --- | --- |
| UC-ORD-C01 | GET | `/api/orders` | 목록 | patientId\|encounterId, encounterType=ER |
| UC-ORD-C01 | GET | `/api/orders/{orderId}` | 단건 | orderId |
| UC-ORD-C02 | POST | `/api/orders` | 생성 | patientId, encounterType=ER, encounterId, orderType, items[], orderedBy |
| UC-ORD-01 | POST | `/api/orders` | 구두처방 | 위 + verbalYn=Y |
| UC-ORD-02 | PUT | `/api/orders/{orderId}` | 수정 | orderId |
| UC-ORD-03 | PATCH | `/api/orders/{orderId}/cancel` | 취소 | orderId, cancelReason |
| UC-ORD-04 | POST | `/api/orders/{orderId}/confirm` | 구두확정 | orderId |
| UC-ORD-C05 | POST | `/api/orders/validate` | DUR | items[] |
| UC-ORD-C05 | GET | `/api/orders/{orderId}/validations` | 검증결과 | orderId |
| UC-ORD-C08 | GET | `/api/orders/{orderId}/history` | 이력 | orderId |
| UC-ORD-C09 | GET | `/api/orders/{orderId}/routes` | 라우팅 | orderId |
| UC-ORD-05 | POST | `/api/orders` | 검사 STAT | orderType=EXAM, priority=STAT, … |
| UC-ORD-07 | POST | `/api/orders` (+route) | 조제 라우팅 | ACTIVE orderId |
| UC-ORD-10 | POST | `/api/orders` | 수술 | orderType=SURGERY (Q-SURGERY) |
| UC-ORD-C10 | POST | `/api/orders/{orderId}/acknowledge` | ACK | W0 Open |

---

## 4. 타 서비스 Consumer (응급이 자주 쓰는 API)

| Provider | Method | URL | 용도 |
| --- | --- | --- | --- |
| PAT | GET | `/api/v1/patients/{patientId}` | 환자 상세 |
| PAT | POST | `/api/v1/patients/batch-query` | 목록 N+1 방지 |
| PAT | GET | `/api/v1/patients/{patientId}/safety-info` | 알레르기 SoT |
| PAT | GET | `/api/v1/patients/{patientId}/guardians/*` | 보호자 |
| ADM | GET | `/api/admin/commonCodes/groups/{groupCode}` | KTAS 등 공유코드 |
| ADM | GET | `/api/admin/medicalDepts` | 진료과 |
| ADM | GET | `/api/staff/employees/{employeeId}` | 직원 |
| RCP | GET | `/receptions/{receptionId}` | 접수 상세 |
| RCP | GET | `/receptions/waiting` | 대기 환자 |
| RCP | PATCH | `/receptions/{receptionId}/status` | 접수 상태 |
| LAB | GET | `/lab-orders/{labOrderId}/results` | 결과 (Provider=LAB) |
| LAB | GET | `/lab-orders/{labOrderId}` | 오더 상태 |

---

## 5. 폐기 또는 변경 (기존 경로)

| 기존 Endpoint | 조치 | 이유 |
| --- | --- | --- |
| `/api/emergency/orders*` (CRUD Provider) | 삭제 또는 BFF→GR2 | SoT=GR2 |
| `.../cancel`, `.../confirm`, verbal | → GR2 동일 동작 | 코어 소유 |
| `/api/emergency/orders/lab-imaging` 생성 | 폐기 | LAB 직통 금지 |
| `.../lab-imaging/{id}/result` | 유지 시 Provider=LAB 명시 | 결과 SoT=LAB |
| `/api/emergency/orders/pharmacy` 직통 | 폐기→GR2 | PHM 직통 금지 |
| `/api/emergency/.../allergy` POST | 제거 | PAT SoT |
| 협진/당직을 orders 하위 | → consultations / on-call-pages | 채널 ≠ 처방 |

---

## 6. 이벤트 구독 (후보)

| 이벤트 | EMG 구독 | 목적 |
| --- | --- | --- |
| order.created | Y | 현황판·투약대기 |
| order.confirmed | Y | 구두확정 후 MAR/조제 |
| order.updated | Y | 목록 동기화 |
| order.cancelled | Y(권장 필수) | 대기 취소 |
| order.routed | Y | LAB/PHM/SUR 알림 |
| order.validation.completed | Y | DUR UI |
| order.acknowledged | 선택 | Q-ACK |

---

## 7. 공통 에러 규격

모든 EMG Provider API는 실패 시 동일한 `ApiResponse` 형태로 응답한다 (`data`는 항상 `null`).

| HTTP | code | 발생 조건 | message 예시 |
| --- | --- | --- | --- |
| 400 | `EMG_BAD_REQUEST` | 필수 값 누락·검증 실패·JSON 파싱 실패 | `encounterId and ktasScore are required` |
| 404 | `EMG_NOT_FOUND` | 존재하지 않는 리소스 id | `bed not found: 999` |
| 409 | `EMG_CONFLICT` | 현재 상태와 모순되는 요청 (사용중 병상/장비 재배정 등) | `bed already occupied: 3` |
| 500 | `EMG_INTERNAL_ERROR` | 처리되지 않은 서버 오류 (내부 메시지 미노출, 서버 로그 확인) | `서버 내부 오류가 발생했습니다.` |

```json
{
  "code": "EMG_NOT_FOUND",
  "message": "bed not found: 999",
  "data": null
}
```

- 401/403(인증·인가)은 아직 인증 체계가 없어 미정의. Security 도입 시 추가한다.
- 처리 위치: `common/config/EmergencyExceptionHandler` (RestControllerAdvice)

---

## 8. 요청/응답 예시

### KTAS 분류

```http
POST /api/emergency/triage/ktas
Content-Type: application/json

{
  "patientId": "P20260001",
  "encounterId": "ER-20260716-001",
  "ktasScore": 2,
  "assessmentTypeCode": "INITIAL",
  "reason": "흉통"
}
```

```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "triageAssessmentId": 1001,
    "ktasLevelCode": "2",
    "assessedAt": "2026-07-16T10:30:00"
  }
}
```

### MAR 기록

```http
POST /api/emergency/care/medication-administrations

{
  "encounterId": "ER-20260716-001",
  "orderId": 55001,
  "orderItemId": 5500101,
  "drugCode": "A12BC",
  "dose": "1g",
  "routeCode": "IV",
  "administeredAt": "2026-07-16T11:05:00"
}
```

### 구두처방 (GR2 Consumer)

```http
POST /api/orders

{
  "patientId": "P20260001",
  "encounterType": "ER",
  "encounterId": "ER-20260716-001",
  "orderType": "DRUG",
  "verbalYn": "Y",
  "items": [{ "drugCode": "A12BC", "dose": "1g", "routeCode": "IV" }],
  "orderedBy": "E0001"
}
```
