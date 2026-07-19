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
  participant IPT as ward / inpatient

  MD->>FE: 퇴실 유형=ADMIT
  FE->>EMG: POST /dispositions
  EMG-->>FE: dispositionId

  FE->>EMG: POST /dispositions/{id}/admission-request
  EMG-->>FE: ADMISSION_REQUEST 저장
  Note over EMG,IPT: IPT/RCP에 수신 POST API 부재<br/>(카탈로그 이슈#6) — 동기 or 이벤트 확정 필요
```

전원 시에는 `transfer-note` 작성 전 GR2 `GET /api/orders`로 투약내역을 조회해 소견서에 반영합니다(스냅샷이 아닌 조회 시점 데이터).

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
