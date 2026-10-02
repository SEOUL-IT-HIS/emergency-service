# API 명세

근거: `응급관리_API_목록(응급전용제외).xlsx` (2026-07-16), `전체서비스_API카탈로그.xlsx`  
Base path (EMG Provider): `/api/emergency`  
공통 응답: `{ "code", "message", "data" }`  
Swagger UI: `http://localhost:8089/swagger-ui.html` (코드 기반 자동 생성 명세 — 상세 스키마는 Swagger 참조)

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
| UC-RES-01 | GET | `/api/emergency/resources/congestion` | 구역별 혼잡도(현황판이 사용, 별도 UC 아님) | - |
| UC-RES-02 | POST | `/api/emergency/resources/bed-assignments` | 병상 배정 | encounterId, bedId |
| UC-RES-02 | GET | `/api/emergency/resources/bed-assignments/current` | 접수의 현재(해제 안 된) 병상 배정. 없으면 `data=null`. 화면이 새로고침·환자 전환 뒤에도 Release 버튼을 보여주는 용도 | receptionId |
| UC-RES-02 | PATCH | `/api/emergency/resources/bed-assignments/{assignmentId}/release` | 병상 수동 해제(병상은 EMPTY로). **퇴실 처리가 끝나면(`DischargeProgress` DONE) 응급이 자동으로 해제한다** — 귀가·사망·자의퇴원은 결정 즉시, 입원은 병동 병상 배정(02) 회신, 전원은 소견서 작성 시점. 자동 해제는 해제자 `SYSTEM` | releasedById |

### 2.3 ER-CARE

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-CARE-01 | GET | `/api/emergency/care/patients` | 응급환자 목록. `status`: `IN_CARE`(진료 중) / `DONE`(퇴실 절차 완료) / 생략 시 전체. 저장값이 아니라 계산값(`DischargeProgress`, 현황판 재실 수·장기체류 알림도 같은 기준) — 귀가·사망·자의퇴원은 결정 즉시, 입원은 병동이 병상 배정(02)해야, 전원은 소견서를 써야 DONE. 환자서비스 장애 시 환자명 없이 목록 반환 | date?, status? |
| (접수 연동) | GET | `/api/emergency/care/patients/active` | 한 환자의 **진행 중(퇴실 처리 전, `DischargeProgress` 기준)인 응급 접수** 목록을 접수 시각 오름차순으로. **접수 후 `sinceHours`(기본 48)시간이 지난 건은 퇴실 누락·테스트 데이터로 보고 제외.** 없으면 빈 목록. 접수 서비스가 새 접수 전에 호출해 같은 환자 중복 접수를 경고하는 용도(경고용 — 차단 여부는 접수 쪽이 정하고, 응급 서버가 응답하지 않으면 접수는 그대로 진행). 응답 `receptionId`, `patientId`, `receivedAt`. 응급은 접수 이벤트를 거절하지 않고 항상 저장하며, 같은 환자의 진행 중 접수가 있으면 경고 로그만 남긴다 | patientId, sinceHours? |
| UC-CARE-02 | POST | `/api/emergency/care/records` | 진료기록 | encounterId, content |
| UC-CARE-03 | GET | `/api/emergency/care/treatments` | 접수 건별 처치기록(시행 시각 순) | receptionId |
| UC-CARE-03 | POST | `/api/emergency/care/treatments` | 처치기록 | encounterId, treatmentCode, orderId?(GR2) |
| UC-CARE-04 | GET | `/api/emergency/care/medication-administrations` | 접수 건별 투여기록(투여 시각 순) | receptionId |
| UC-CARE-04 | POST | `/api/emergency/care/medication-administrations` | MAR | encounterId, **orderId(GR2)**, administeredAt, dose |
| UC-CARE-05 | GET | `/api/emergency/care/cpr-timelines` | 접수 건별 CPR 기록(최신 시작 순, 타임라인은 이벤트 시각 순) | receptionId |
| UC-CARE-05 | POST | `/api/emergency/care/cpr-timelines` | CPR | encounterId, events[] |
| UC-CARE-06 | POST | `/api/emergency/care/consents` | 동의 기록(종이 동의서 수령 사실만, 서명·파일 없음. 유예=DEFERRED+reason) | encounterId, consentTypeCode, consentStatusCode, consentedByCode, recordedById |
| UC-CARE-06 | GET | `/api/emergency/care/consents` | 접수 건별 동의 기록(수령 일시 최신순) | receptionId |

### 2.4 ER-CHANNEL

> **범위 조정 (2026-09-30, UML `01-5 최종`)**: 협진 요청·당직의 호출·수술-시술 긴급 요청·외부 병원 전원 정보 조회·응급 의료기기 할당·구급차 이송 기록·응급업무코드 CRUD는 프로젝트 기간 단축으로 **범위에서 제외**했다(코드 삭제 완료). 혼잡도 지표는 종합 현황판에 통합, 동의는 종이 원본 수령 사실만 기록하는 `동의 기록` 1개로 축소.

협진(`/consultations`)·당직(`/on-call-pages`)·수술 요청 API는 삭제했다. 이 채널에 남은 API는 없다.

### 2.5 ER-MONITOR

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-MON-01 | GET | `/api/emergency/monitor/dashboard` | 종합 현황판. 재실 환자 = 퇴실 처리가 끝나지 않은 접수(입원 병상 대기 포함), 퇴실 완료 환자의 알림은 제외 | - |
| UC-MON-02 | GET | `/api/emergency/monitor/long-stay-alerts` | 장기체류 알림(미확인, 재실 환자만) | thresholdHours? |
| UC-MON-02 | PATCH | `/api/emergency/monitor/long-stay-alerts/{alertId}/acknowledge` | 알림 확인 처리(없는 알림 404, 이미 확인 409) | alertId, acknowledgedById |

### 2.6 ER-DISPOSITION

| UC | Method | Endpoint | 설명 | 필수 파라미터 |
| --- | --- | --- | --- | --- |
| UC-DISP-01 | POST | `/api/emergency/dispositions` | 퇴실 결정. 이미 결정이 있으면 '결정 변경'(새 행 추가, 최신 결정 적용) — 후속 조치 전(입원요청 없음·거부됨, 전원 소견서 없음)일 때만 허용, 병동 회신 대기 중·퇴실 완료면 409, 같은 유형으로 다시 결정하면 400 | encounterId, dispositionType |
| UC-DISP-01 | GET | `/api/emergency/dispositions` | 접수 건별 퇴실 결정 이력(최신이 첫 번째). `changeable`: 최신 결정을 바꿀 수 있는지 | receptionId |
| UC-DISP-02 | GET | `/api/emergency/dispositions/{id}/admission-requests` | 입원요청 이력(최신이 첫 번째). 상태(`requestStatusCode` 01 요청됨 / 02 병상 배정 완료 / 03 거부)는 병동 회신(Kafka)으로 갱신, 처음 회신만 반영. `assignedWardCode`: 병동이 실제 배정한 병동(WARD_CD, 02일 때만 값) | id |
| UC-DISP-02 | POST | `/api/emergency/dispositions/{id}/admission-request` | 입원 요청(최신 퇴실 결정이 입원일 때만 — 바뀐 이전 결정이면 409, 요청됨·배정 완료 상태가 있으면 409, note 500자 이하, DB 커밋 뒤 병동으로 Kafka 발행) | id, targetDeptCode?(DEPT_CD), wardPrefer?(WARD_CD), note? |
| UC-DISP-03 | GET | `/api/emergency/dispositions/{id}/transfer-notes` | 전원 소견서 목록(최신이 첫 번째) | id |
| UC-DISP-03 | POST | `/api/emergency/dispositions/{id}/transfer-note` | 전원 소견서(최신 퇴실 결정이 전원일 때만 — 바뀐 이전 결정이면 409) | id, targetHospitalCode, content, writtenById |

### 2.7 ER-CODE (응급 전용)

> 응급업무코드 조회·등록·수정·사용여부 변경은 **범위에서 제외**(2026-09-30). `/api/emergency/codes` 삭제, 코드는 admin 공통코드로 일원화(캐시 우선·없으면 코드 안 폴백).

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
| UC-ORD-C10 | POST | `/api/orders/{orderId}/acknowledge` | ACK | W0 Open |

---

## 3-1. 응급 처방 연동 — 처방코어(OPD) 호출 (검사·약품만)

처방 원장은 **처방코어(OPD, outpatient-service)** 가 소유한다. 응급은 서버 대 서버로 호출하는 **BFF 프록시**만 두고(`/api/emergency/orders*`), 처방 내용은 응급 DB에 저장하지 않는다(`orderId` 참조만). 위 3장의 `/api/orders*` 표는 초기 계획이고, 실제 처방코어 API는 아래다(2026-10-01 처방코어 회신 기준).

| Method | Endpoint (응급) | 처방코어 호출 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/emergency/orders` | `POST /api/outpatient/prescriptions/emergency/{receptionId}` | 검사·약품 처방 등록. 본문 `encounterId`(접수ID), `prescribedBy`, `priorityCode`(ORDER_PRIORITY_CD, STAT=01), `timingCode`(ORDER_TIMING_CD 01/02/03), `items[]`, 선택 `verbalYn`(Y/N), `dispatchNow`. `patientId`·`serviceType="ER"`·`departmentCode="10"`·`orderMethod="01"`은 서버가 채움 |
| GET | `/api/emergency/orders?encounterId=` | `GET /api/outpatient/prescriptions?receptionId=` | 접수의 처방 목록(최근 처방 먼저). items 없는 가벼운 목록 — 처방ID·상태·`priorityCode`와 전송 상태 요약(`labSendStatus`: 검사 항목 중 하나라도 FAILED면 FAILED, 미전송/PENDING이 있으면 PENDING, 전부 SENT면 SENT, 검사 항목이 없으면 null / `pharmacySendStatus`: 처방 단위 PENDING·SENT·FAILED). 상세·검사결과는 단건 조회 |
| GET | `/api/emergency/orders/lab-items?name=` | `GET /api/outpatient/prescriptions/lab-items/search?name=` | 검사항목 검색(처방 등록 때 항목 선택). LAB팀 계약 그대로 `itemCode`, `itemName`, `testClassification`(GENERAL/MICROBIOLOGY/PATHOLOGY), `specimenTypes`. `name` 생략 시 전체, 있으면 코드/이름 부분일치 |
| GET | `/api/emergency/orders/{orderId}` | `GET /api/outpatient/prescriptions/{id}` | 처방 단건 |
| PATCH | `/api/emergency/orders/{orderId}/verbal-confirm` | `PATCH …/{id}/verbal-confirm?confirmedBy=` | 구두처방 사후 확정(본문 `confirmedBy`=의사 ID, 확정 일시·확정자 기록). 구두처방(`verbalYn=Y`)이 아니거나 이미 확정된 처방은 처방코어가 거절 — 409는 409로, 그 외 4xx는 400으로 전달 |
| PATCH | `/api/emergency/orders/{orderId}/cancel` | `PATCH …/{id}/deactivate?cancelReason=&userId=` | 취소(삭제 아님). **수정 API는 없음 — 변경은 취소 후 재등록** |
| POST | `/api/emergency/orders/{orderId}/dispatch-lab` | `POST …/{id}/dispatch-lab` | 검사(LAB) 전송. 자동 호출이 아니라 응급이 직접 호출 |
| POST | `/api/emergency/orders/{orderId}/dispatch-pharmacy` | `POST …/{id}/dispatch-pharmacy` | 약제(PHM) 전송 |

- **영상(방사선) 오더는 제외**: 처방코어가 받지 않는다. `items[].prescriptionType`은 `"검사"`·`"약품"`만 허용(그 외 400).
- `dispatchNow=true` 이면 등록 직후 검사 항목은 `dispatch-lab`, 약품 항목은 `dispatch-pharmacy`까지 호출한다. 등록은 이미 끝났으므로 전송이 실패해도 되돌리지 않고 응답의 `labDispatchStatus`/`pharmacyDispatchStatus`로 알리며, 전송 API로 다시 시도한다. 값은 `SENT`/`FAILED`/`PENDING`/`REQUESTED`(상태를 못 읽음)/`NOT_APPLICABLE`(해당 항목 없음).
- **전송 호출 성공(HTTP 200)과 실제 전송 성공은 다르다.** 실서버(처방코어 개발 서버)에서 `dispatch-lab`이 200을 주고도 검사 항목이 `FAILED`(labOrderId 없음)로 남는 것을 확인했다. 그래서 응급은 전송 호출 직후 처방을 다시 읽어 **실제 전송 상태**를 돌려주고(`dispatch-lab`/`dispatch-pharmacy` 응답의 `status`), 처방코어가 비동기로 상태를 바꾸는 경우를 위해 화면은 전송·등록 뒤 목록을 다시 불러온다.
- **구두처방**(2026-10-02 처방코어 안내 반영): `verbalYn=Y`로 등록하면 `orderMethod=02`(구두)로, 아니면 `01`(전자)로 보내고(ADM `ORDER_METHOD_CD`: 01 Electronic / 02 Verbal / 03 Telephone), `verbalYn`(Y/N)도 같이 전달한다(`app.order.forward-verbal-yn`, 기본 true — 문제가 생기면 false로 끌 수 있다). 사후 확정은 위 `verbal-confirm`. 응답에 `orderMethodName`(예: Verbal)과 확정 일시·확정자(`verbalConfirmedAt`/`verbalConfirmedBy`)가 온다. **처방코어 배포 전이라 실서버 연동은 미확인**.
- 우선순위·시점 코드는 처방코어가 잘못된 값도 그대로 저장하므로 **응급이 호출 전에 검증**(admin 공통코드, 없으면 폴백)한다.
- 오류: 처방코어 4xx → 400(메시지 포함), 404 → 404, 5xx·타임아웃·연결 실패 → **502** `EMG_UPSTREAM_ERROR`.
- 설정: `app.order.base-url`(기본 `http://localhost:8088`, 환경변수 `ORDER_BASE_URL`), `app.order.department-code`(`10`), `app.order.forward-verbal-yn`(`false`).
- 처방 목록은 처방코어의 `GET …/prescriptions?receptionId=`(응답 필드 확정, **처방코어 배포 전** — 배포 후 실서버 확인 필요)를 호출한다. 응급 화면은 환자를 고르면 이 목록을 불러오고, 투약(MAR)·처치 기록의 `orderId`는 이 목록에서 고른다. 검사결과·조제상태는 Kafka(토픽 확정 대기)로 상태만 받고 결과 본문은 단건 조회로 읽는다(응급 DB에 결과 본문을 저장하지 않는다).

---

## 4. 타 서비스 Consumer (응급이 자주 쓰는 API)

| Provider | Method | URL | 용도 |
| --- | --- | --- | --- |
| PAT | GET | `/api/patients/{patientId}` | 환자 상세 |
| PAT | POST | `/api/patients/batch-query` | 목록 N+1 방지 |
| PAT | GET | `/api/patients/{patientId}/safety-info` | 알레르기 SoT |
| PAT | GET | `/api/patients/{patientId}/guardians/*` | 보호자 |
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
    "triageAssessmentId": "550e8400-e29b-41d4-a716-446655440000",
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
  "orderId": "3f2b8c1e-7a4d-4e5b-9c61-2d8f0a1b5e77",
  "orderItemId": "9a6e41d0-5c2b-4b8f-8e3a-71c0d4f2a955",
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
