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

## 1-1. 로그인과 처리자 ID

로그인은 admin이 처리한다. admin이 Redis에 세션(`SessionUser`: empId, empName …)을 저장하고 브라우저에 `SESSION` 쿠키를 주면, 응급은 같은 쿠키로 같은 세션을 읽는다(`spring-session-data-redis`). 세션 속성 이름은 몰라도 `SessionUser` 타입으로 찾는다. 세션이 없거나 읽을 수 없으면(로그인 안 함, 만료) "로그인 정보 없음"으로 본다. 참고: Redis가 응답하지 않으면 SESSION 쿠키가 있는 요청은 Spring MVC가 요청 맨 앞에서 세션을 읽다가 500이 난다 — 응급 코드 이전의 프레임워크 동작이라 응급만의 문제가 아니며, Redis가 살아 있는 서버에서는 해당 없다.

**처리자 ID는 화면이 보낸 값이 아니라 세션의 empId로 기록한다.** 요청에 실린 값은 바꿔 보낼 수 있어서, 세션이 있으면 요청값을 무시하고 로그인 사용자로 덮어쓴다(다르면 서버 로그에 남긴다). 세션이 없는 환경(단독 실행 등)에서만 요청값을 그대로 쓴다.

| 로그인 사용자로 기록(요청값 무시) | 요청값 그대로(화면에서 직원을 골라 보냄) |
| --- | --- |
| 진료기록 `recordedById`, 동의 `recordedById`, CPR 이벤트 `recordedById`, KTAS `assessedById`(분류·재평가), 병상 `assignedById`·`releasedById`, 장기체류 경고 `acknowledgedById`, 처방 취소 `userId` | **의사를 지정**: 처방 `prescribedBy`, 구두처방 확정 `confirmedBy`, 퇴실 결정 `decidedById`, 격리 결정 `decidedById`, 전원소견서 `writtenById`<br>**실제로 행위를 한 사람**(기본은 로그인한 사람, 다른 의사·간호사를 고를 수 있음): 처치 `performedById`, 투약 `administeredById`, 활력징후 `measuredById`, 위험 스크리닝 `screenedById` |

의사 칸은 로그인한 사람이 아니라 선택한 의사(구두처방은 간호사가 의사 대신 입력)라서 요청값을 쓴다. 처치·투약·활력징후·스크리닝은 의사가 시술하고 간호사가 기록하거나 한 간호사가 투여하고 다른 간호사가 기록하는 경우가 있어 실제로 행위를 한 직원을 고르게 하고, 기록 책임(진료기록·동의·CPR·KTAS 등)은 로그인한 사람으로 고정한다. 의사·간호사 목록은 admin 직원의 부서(`DEPT_CD`)별로 묶어 응급의학과(10)를 맨 위에 보여주되 다른 과도 막지 않는다(협진·당직). 서버가 그 값이 실제 의사인지는 확인하지 않는다(admin 직원 조회가 사용자 세션을 요구한다 — 이후 단계).

**로그인 검사(`app.auth.required`, 환경변수 `AUTH_REQUIRED`, 기본 `false`)**: `true`면 `/api/emergency/**`를 로그인 없이 부를 때 **401** `EMG_UNAUTHENTICATED`를 준다. 아래 두 API는 접수 서비스가 서버끼리 부르는 것이라 쿠키가 없으므로 제외한다(CORS 사전 요청 `OPTIONS`도 통과).

| 제외 API | 호출처 |
| --- | --- |
| `GET /api/emergency/care/patients/active` | 접수 — 중복 접수 확인 |
| `POST /api/emergency/care/reception-intakes` | 접수 — 접수 정보 REST 전송(정식 경로는 Kafka) |

켜기 전에 admin과 같은 Redis 세션을 읽는 환경에서 **로그인한 브라우저로** 확인한다. 공용 axios가 401을 받으면(로그인 상태였을 때) 로그인 화면으로 보낸다.

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

## 2-1. 퇴실 완료(DONE) 환자의 입력 제한

퇴실 처리가 끝난(`DischargeProgress` DONE — 귀가·사망·자의퇴원은 결정 즉시, 입원은 병동 병상 배정 회신, 전원은 소견서 작성) 접수에는 새로 배치·평가·처방하지 못한다. 막힌 요청은 **409** `EMG_CONFLICT`(`reception already discharged: <접수ID>`)로 응답한다. 병동 회신을 기다리는 중(WAITING_WARD)이나 후속 조치 전(OPEN)은 아직 응급실에 있으므로 막지 않는다.

| 구분 | 퇴실 완료 후 | API |
| --- | --- | --- |
| 막음(409) | 병상 배정 | `POST /resources/bed-assignments` |
| 막음(409) | KTAS 분류·재평가, 활력징후, 격리 등록, 위험 스크리닝 | `POST/PUT /triage/ktas`, `POST /triage/vital-assessments`, `POST /triage/infection-isolations`, `POST /triage/risk-screenings` |
| 막음(409) | 처방 등록 | `POST /orders` (처방코어 호출 없음) |
| 허용 | 사후 기록 | 진료기록·처치·투약(MAR)·CPR·동의 |
| 허용 | 정리 작업 | 병상 해제, 격리 해제, 처방 취소·전송·구두 확정 |

- 화면이 미리 막도록 `GET /dispositions`·`POST /dispositions` 응답의 최신 결정에 `stage`(NONE/OPEN/WAITING_WARD/DONE)를 내려준다. 화면은 DONE이면 위 패널의 등록 버튼을 비활성화하고 안내를 보여 준다.
- 이유: 퇴실한 환자에게 병상을 배정하면 퇴실 이벤트가 다시 오지 않아 병상이 영구 점유로 남는다.

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
| POST | `/api/emergency/orders/{orderId}/dispatch-pharmacy` | `POST …/{id}/dispatch-pharmacy` | 약제(PHM) 전송. **기본 비활성(`app.order.pharmacy-enabled=false`) — 호출하면 409 `EMG_CONFLICT`** (아래 "약제 제외" 참고) |

- **영상(방사선) 오더는 제외**: 처방코어가 받지 않는다. `items[].prescriptionType`은 `"검사"`·`"약품"`만 허용(그 외 400).
- `dispatchNow=true` 이면 등록 직후 검사 항목은 `dispatch-lab`, 약품 항목은 `dispatch-pharmacy`까지 호출한다(**약제 전송이 꺼져 있으면 약품은 호출하지 않고 `pharmacyDispatchStatus=NOT_APPLICABLE`**). 등록은 이미 끝났으므로 전송이 실패해도 되돌리지 않고 응답의 `labDispatchStatus`/`pharmacyDispatchStatus`로 알리며, 전송 API로 다시 시도한다. 값은 `SENT`/`FAILED`/`PENDING`/`REQUESTED`(상태를 못 읽음)/`NOT_APPLICABLE`(해당 항목 없음).
- **전송 호출 성공(HTTP 200)과 실제 전송 성공은 다르다.** 실서버(처방코어 개발 서버)에서 `dispatch-lab`이 200을 주고도 검사 항목이 `FAILED`(labOrderId 없음)로 남는 것을 확인했다. 그래서 응급은 전송 호출 직후 처방을 다시 읽어 **실제 전송 상태**를 돌려주고(`dispatch-lab`/`dispatch-pharmacy` 응답의 `status`), 처방코어가 비동기로 상태를 바꾸는 경우를 위해 화면은 전송·등록 뒤 목록을 다시 불러온다.
- **구두처방**(2026-10-02 처방코어 안내 반영): `verbalYn=Y`로 등록하면 `orderMethod=02`(구두)로, 아니면 `01`(전자)로 보내고(ADM `ORDER_METHOD_CD`: 01 Electronic / 02 Verbal / 03 Telephone), `verbalYn`(Y/N)도 같이 전달한다(`app.order.forward-verbal-yn`, 기본 true — 문제가 생기면 false로 끌 수 있다). 사후 확정은 위 `verbal-confirm`. 응답에 `orderMethodName`(예: Verbal)과 확정 일시·확정자(`verbalConfirmedAt`/`verbalConfirmedBy`)가 온다. 처방코어 개발 서버(2026-10-02~03)에서 등록·목록·단건·구두 확정·취소·검사 전송까지 확인했다.
- 우선순위·시점 코드는 처방코어가 잘못된 값도 그대로 저장하므로 **응급이 호출 전에 검증**(admin 공통코드, 없으면 폴백)한다.
- 오류: 처방코어 4xx → 400(메시지 포함), 404 → 404, 5xx·타임아웃·연결 실패 → **502** `EMG_UPSTREAM_ERROR`.
- 설정: `app.order.base-url`(기본 `http://localhost:8088`, 환경변수 `ORDER_BASE_URL`), `app.order.department-code`(`10`), `app.order.forward-verbal-yn`(`false`).
- **약제 제외(약제 서비스 미참여)**: 약제(PHM) 서비스가 이번 범위에서 빠져 약제 전송은 쓰지 않는다. 전송해도 받는 곳이 없는데 처방코어는 `SENT`로 표시해 약제로 넘어간 것처럼 보이기 때문이다(개발 서버에서 확인). `app.order.pharmacy-enabled`(기본 `false`)가 꺼져 있으면 등록 직후 전송에서 약품을 건너뛰고(`NOT_APPLICABLE`), `POST …/dispatch-pharmacy`는 409로 거절한다. 검사(LAB) 전송은 그대로다. **약품 처방 등록과 투약(MAR) 기록은 그대로 쓴다** — 투약 기록은 처방(`orderId`)이 필요하고, 처방 등록은 처방코어에 저장만 한다(약제 Kafka와 무관). 약품 검색(`GET …/medications/search`)은 처방코어가 약제 서비스에 의존해 500(`OPD999`)이라 쓰지 않고 약품 코드·이름을 직접 입력한다. 약제 조제 상태 조회(UC-ORD-08)와 조제 요청 연동(UC-ORD-07)은 범위 제외. 약제 서비스가 돌아오면 `pharmacy-enabled=true`(백엔드)와 프론트 `PHARMACY_DISPATCH_ENABLED=true`로 다시 켠다.
- 처방 목록은 처방코어의 `GET …/prescriptions?receptionId=`를 호출한다(개발 서버에서 확인). 응급 화면은 환자를 고르면 이 목록을 불러오고, 투약(MAR)·처치 기록의 `orderId`는 이 목록에서 고른다.
- **검사 결과는 응급 DB에 저장하지 않고 처방 단건 조회에서 그대로 전달한다.** 항목(`items[]`)에 `resultReportedAt`과 `resultDetails[]`(`detailName`, `resultValue`, `resultUnit`, `referenceRange`, `abnormalFlag` L/H/N)가 온다. 결과는 LAB → 처방코어로 비동기로 도착하므로 화면은 결과가 올 때까지 30초마다(최대 10분) 목록을 다시 불러오고, 그 뒤에는 Refresh로 직접 확인한다. 응급이 LAB 결과 토픽(`lab.lab-result.reported.v1`)을 직접 구독하지는 않는다(결과 본문을 저장하지 않는 규칙상 보여줄 곳이 없다).
- **결과를 받는 검사는 01~04뿐이다**(01 Blood Glucose, 02 CBC, 03 Liver Function, 04 Urinalysis). 05 Blood Culture, 06 Urine Culture, 07 Histopathology, 08 Cytology(배양·병리)는 받는 결과항목이 없고 처방코어도 반영하지 않는다(전체 MSA 카탈로그 점검 I-04). 화면은 이 검사에 "결과 대기" 대신 안내만 보이고 자동 새로고침 대상에서도 뺀다. **수술·영상 오더는 이번 범위에서 제외**한다.
- **LAB 거절 사유**: 검사 전송이 `FAILED`일 때 항목의 `rejectReason`을 그대로 전달한다(예: "유효하지 않은 환자ID입니다" = LAB에 없는 환자, "이미 접수된 오더입니다" = 중복 전송). 중복 전송은 처방코어가 항목을 FAILED로 덮어쓰지만 LAB은 이미 받은 상태이므로 `labOrderId`가 있거나 거절 사유가 "이미 접수"이면 전송 완료(SENT)로 계산한다.
- **검사항목 목록은 응급 서버가 10분간 캐시**한다(`app.order.lab-item-cache-minutes`, 기본 10). 처방코어가 일시적으로 응답하지 못하면 마지막으로 받은 목록을 대신 돌려준다(캐시가 없을 때만 502). 이름·코드 필터는 응급 서버에서 한다.

---

## 4. 타 서비스 Consumer (응급이 자주 쓰는 API)

| Provider | Method | URL | 용도 |
| --- | --- | --- | --- |
| PAT | GET | `/api/patients/{patientId}` | 환자 상세 |
| PAT | POST | `/api/patients/batch-query` | 목록 N+1 방지 |
| PAT | GET | `/api/patients/{patientId}/safety-info` | 알레르기 SoT |
| PAT | GET | `/api/patients/{patientId}/guardians/*` | 보호자 |
| ADM | GET | `/api/admin/commonCodeGroup/list`, `/api/admin/commonCodeItem/list?groupId=` | 공통코드(기동 시 전체 캐시). 구경로 `/api/commonCodeGroup/list`는 admin이 제거 예정이라 신경로 사용 |
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
