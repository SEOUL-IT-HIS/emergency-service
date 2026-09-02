# 업무 흐름 및 시퀀스

근거: UML 유스케이스, API 목록, MSA 경계

## 1. 전체 환자 여정

```mermaid
flowchart LR
  A[내원/접수 RCP] --> B[EMS·Triage]
  B --> C[병상·기기 배정]
  C --> D[진료·처방·투여]
  D --> E[모니터링]
  E --> F{퇴실 결정}
  F -->|귀가| G[Disposition HOME]
  F -->|입원| H[입원요청 → IPT]
  F -->|전원| I[소견서·이송]
  F -->|사망/DAMA| J[Disposition 기록]
```

---

## 2. 상태평가 (Triage) 시퀀스

```mermaid
sequenceDiagram
  actor Nurse as 간호/분류간호사
  participant FE as Emergency FE
  participant EMG as emergency-service
  participant EMS as EMS(119)
  participant ADM as admin-service

  Nurse->>FE: EMS 사전정보 조회
  FE->>EMG: GET /triage/ems-info
  EMG->>EMS: 외부 규격 조회(또는 수신 저장)
  EMG-->>FE: EMS_REFERRAL 데이터

  Nurse->>FE: KTAS 분류 입력
  FE->>ADM: GET commonCodes KTAS (표시명)
  FE->>EMG: POST /triage/ktas
  EMG-->>FE: TRIAGE_ASSESSMENT 저장

  Nurse->>FE: 활력징후/스크리닝
  FE->>EMG: POST vital-assessments / risk-screenings
  EMG-->>FE: 저장 결과
```

격리 등록 시 DUR 이력은 EMG가 쓰지 않고 GR2/PHM을 **조회(Consumer)** 합니다.

---

## 3. 병상 배정

```mermaid
sequenceDiagram
  actor Staff as 응급실 스태프
  participant FE as Emergency FE
  participant EMG as emergency-service

  Staff->>FE: 가용 병상 확인 / 배정
  FE->>EMG: GET /resources/congestion
  EMG-->>FE: 과밀화·점유 지표
  FE->>EMG: POST /resources/bed-assignments
  Note over EMG: BED_ASSIGNMENT 생성<br/>BED.bed_status_code 갱신
  EMG-->>FE: 배정 결과
```

---

## 4. 구두처방 → 확정 → MAR (핵심)

처방 SoT는 GR2, 투여 기록은 EMG입니다.

```mermaid
sequenceDiagram
  actor MD as 응급의
  actor RN as 간호사
  participant FE as Emergency FE
  participant GR2 as GR2 처방코어
  participant EMG as emergency-service
  participant PHM as pharmacy-service

  MD->>FE: 구두처방 입력
  FE->>GR2: POST /api/orders (verbalYn=Y, encounterType=ER)
  GR2-->>FE: orderId (VERBAL)
  GR2--)EMG: event order.created (구독 시)

  MD->>FE: 사후 확정
  FE->>GR2: POST /api/orders/{id}/confirm
  GR2-->>FE: ACTIVE
  GR2--)EMG: event order.confirmed
  GR2->>PHM: route (조제 필요 시)

  RN->>FE: 투여 기록(MAR)
  FE->>EMG: POST /care/medication-administrations
  Note over EMG: orderId=GR2 참조 필수<br/>처방 원장 복제 금지
  EMG-->>FE: MAR 저장
```

**트레이드오프:** 처방·투여를 서비스로 분리하면 일관성 검증(투여 가능 상태)이 분산됩니다. 대신 이중기입·채널별 처방 원장 난립을 막을 수 있습니다. 확정 전 MAR은 정책으로 차단하는 것이 안전합니다.

---

## 5. 검사 STAT 오더 → 결과 조회

```mermaid
sequenceDiagram
  actor MD as 응급의
  participant FE as Emergency FE
  participant GR2 as GR2 처방코어
  participant LAB as lab-imaging-service
  participant EMG as emergency-service

  MD->>FE: 검사 STAT 오더
  FE->>GR2: POST /api/orders (EXAM, STAT, ER)
  GR2->>LAB: POST /lab-orders (코어 경유)
  Note over FE,LAB: EMG→LAB 직통 생성 금지

  MD->>FE: 결과 조회
  alt BFF/직조회 (Q-RESULT-PATH)
    FE->>LAB: GET /lab-orders/{id}/results
  else EMG 캐시
    FE->>EMG: (선택) 결과 참조 조회
    EMG-->>FE: LAB_IMAGING_RESULT_REF
  end
```

---

## 6. 협진 / 당직 (채널 ≠ 처방)

```mermaid
sequenceDiagram
  actor MD as 응급의
  participant FE as Emergency FE
  participant EMG as emergency-service
  participant OPD as outpatient / inpatient
  participant ADM as admin-service

  MD->>FE: 협진 요청
  FE->>EMG: POST /consultations
  EMG-->>FE: CONSULT_REQUEST
  Note over EMG,OPD: OPD /api/outpatient/referrals 와<br/>통합 여부 Open (이슈#7)

  MD->>FE: 당직 호출
  FE->>EMG: POST /on-call-pages
  EMG->>ADM: 알림 대행? (역할 미확정)
  EMG-->>FE: ONCALL_REQUEST
```

---

## 7. 퇴실 → 입원 요청

```mermaid
sequenceDiagram
  actor MD as 응급의
  participant FE as Emergency FE
  participant EMG as emergency-service

  MD->>FE: 퇴실 유형=ADMIT
  FE->>EMG: POST /dispositions
  EMG-->>FE: dispositionId

  FE->>EMG: POST /dispositions/{id}/admission-request
  EMG-->>FE: ADMISSION_REQUEST 저장
  Note over EMG: 저장 후 Kafka 이벤트 발행 (7-1장)<br/>IPT/RCP 동기 수신 API는 만들지 않음 (카탈로그 이슈#6 해결)
```

전원 시에는 `transfer-note` 작성 전 GR2 `GET /api/orders`로 투약내역을 조회해 소견서에 반영합니다(스냅샷이 아닌 조회 시점 데이터).

---

## 7-1. 입원요청 이벤트 코레오그래피 (Kafka)

> **결정**: 카탈로그 이슈#6은 **이벤트 기반 코레오그래피**로 확정. 별도 오케스트레이터 서비스는 두지 않음 — 각 서비스가 정해진 토픽만 구독/발행.
> **우선순위**: 환자등록 → 접수 → 외래/응급 진료 연결(REST) 골격을 먼저 구축한 뒤 착수. 지금은 **설계만 확정, 구현은 보류**.

```mermaid
sequenceDiagram
  participant EMG as emergency-service
  participant Kafka as Kafka
  participant RCP as reception-service
  participant IPT as ward / inpatient-service

  EMG->>Kafka: publish ADMISSION_REQUESTED<br/>{encounterId, patientId, dispositionId, diagnosis, wardPref, isolationYn, orderedBy}
  Kafka-->>RCP: consume ADMISSION_REQUESTED
  RCP->>RCP: 입원 수속 (보험자격·서약서)
  RCP->>Kafka: publish REGISTRATION_COMPLETED<br/>{dispositionId, patientId, approved}
  Kafka-->>IPT: consume REGISTRATION_COMPLETED
  IPT->>IPT: 병상 예약(RESERVED) → 배정 확정(OCCUPIED)
  IPT->>Kafka: publish BED_ASSIGNED | ADMISSION_REJECTED
  Kafka-->>EMG: consume BED_ASSIGNED | ADMISSION_REJECTED
  EMG->>EMG: dispositionStatus 완료 처리<br/>(응급실 자체 BED* 반납)
```

- 병상 **가용 여부 조회**(`GET`, IPT)는 이 이벤트 흐름과 별개로 계속 **동기 REST 유지** — 쓰기 없음, 참고용 사전 체크(수속 헛수고 방지 목적).
- 응급이 병동 자원에 직접 쓰기(배정)하는 경로는 없음 — 병상 예약·배정 쓰기는 IPT 소유.
- 응급은 원무/병동의 API 주소를 몰라도 됨 — 토픽 이름과 메시지 스펙만 계약(contract)으로 관리.
- 응급에게 최종 결과(`BED_ASSIGNED`/`ADMISSION_REJECTED`) 통지는 필수 — fire-and-forget 금지(퇴실 완료 처리·응급실 병상 반납 타이밍에 필요).

---

## 8. 현황판·이벤트 동기화

```mermaid
flowchart TB
  subgraph EMG
    DASH[monitor/dashboard]
    LOS[long-stay-alerts]
    CARE[patients / MAR / treatments]
  end
  subgraph Events
    E1[order.created]
    E2[order.confirmed]
    E3[order.cancelled]
    E4[order.routed]
  end
  E1 --> DASH
  E2 --> CARE
  E3 --> CARE
  E4 --> DASH
  LOS --> DASH
```

---

## 9. 프론트 상태 흐름 (Saga)

```text
Component ──dispatch──► Slice(*Request)
                            │
                         Saga (axios)
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
         EMG API      GR2 /api/orders   PAT/LAB/ADM
              │             │             │
              └──── *Success / *Failure ──┘
                            │
                      Selector → UI
```

컴포넌트에서 axios 직접 호출은 금지합니다 (`개발표준가이드` 10·11장).

---

## 10. 구현 시 주의 (안티패턴)

| 안티패턴 | 올바른 방향 |
| --- | --- |
| EMG DB에 처방 원장 테이블 재도입 | GR2만 SoT, `order_id` 참조 |
| LAB/PHM 오더 POST를 EMG에서 직통 | GR2 생성 + 코어 라우팅 |
| 환자명·약명을 컬럼에 복제 | 식별자 + API/배치 조회 |
| 알레르기를 EMG에 POST | PAT safety-info만 |
| 협진을 `/orders` 하위 API로 구현 | `/consultations` 분리 |
| 동의 PDF를 EMG에 저장 | Admin 양식 + 동의여부만 |
