# 아키텍처 및 설계

근거: `개발표준가이드.md`, `응급관리_API_목록`, `전체서비스_API카탈로그`, `ER_HIS_dbdiagram_oracle_1.dbml`

## 1. 시스템 전체 구성

```text
[Browser]
   │  HTTPS
[Next.js Frontend] ── emergency 영역 (features/emergency, components/emergency)
   │  REST API (JSON)  ※ BFF 없음 원칙 / 필요 시 프록시 허용
[Spring Boot MSA]
   ├─ emergency-service (EMG / UD2)  ← 본 서비스
   ├─ order-core (GR2 처방코어)
   ├─ patient / reception / lab-imaging / pharmacy / surgery
   ├─ inpatient(ward) / outpatient / billing / admin
   │
[Oracle DB] ── 서비스별 스키마·테이블 분리 (직접 크로스 DB 참조 금지)
```

## 2. EMG Bounded Context

| 컴포넌트 | 책임 | 소유 데이터 |
| --- | --- | --- |
| ER-TRIAGE | 중증도·활력·격리·스크리닝·EMS | TRIAGE_*, EWS, ISOLATION, RISK, EMS_REFERRAL |
| ER-RESOURCE | 병상·기기·혼잡도 | BED*, EQUIPMENT* |
| ER-CARE | EMR·처치·MAR·CPR | CLINICAL_NOTE, TREATMENT, MAR, CPR_* |
| ER-CHANNEL | 협진·당직·수술요청(채널) | CONSULT/ONCALL/SURGERY_REQUEST |
| ER-MONITOR | 현황판·LOS | LOS_ALERT (+집계) |
| ER-DISPOSITION | 퇴실·입원요청·전원·이송 | DISPOSITION, ADMISSION_*, TRANSFER, AMBULANCE |
| ER-CODE | 응급 전용 코드 | EMG_CODE_GROUP, EMG_CODE |
| ER-RESULT(참조) | 결과/조제 캐시·링크 | LAB_IMAGING_RESULT_REF, PHARMACY_STATUS_REF |

처방 원장(CLINICAL_ORDER 등)은 **EMG DB에서 제거·이관**되었으며 GR2 소유입니다.

## 3. Source of Truth (SoT) 규칙

| 데이터 | SoT | EMG 역할 |
| --- | --- | --- |
| 처방/오더 생명주기 | GR2 `/api/orders*` | Consumer (UI·이벤트 구독) |
| 약물 투여(MAR)·처치 기록 | EMG | Provider (`orderId` 참조) |
| 검사·영상 결과 본문 | LAB | Consumer / 캐시 가능 |
| 환자·알레르기·안전정보 | PAT | 조회만 (쓰기 금지) |
| 공통코드(KTAS명·진료과·약품·KCD) | ADM | 조회만 |
| 응급 전용 업무코드 | EMG | Provider |
| 문서 빈 양식(PDF) | ADM `DOCUMENT_TEMPLATE` | 출력·동의여부만 (미구현) |
| 동의 여부 | 업무 서비스 | 전자문서 파일 미저장 |

**금지:** `/api/emergency/orders*` 를 처방 Provider로 두는 것 · EMG→LAB/PHM/SUR 직통 오더 생성 · 알레르기 dual-write

## 4. 서비스 간 통신

```text
                    ┌──────────────┐
     처방 CRUD ──────►│ GR2 Order    │── route ──► LAB / PHM / SUR
                    │ Core         │
                    └──────────────┘
                           ▲
                           │ Consumer
┌──────────┐         ┌─────┴──────┐         ┌──────────┐
│ Reception│◄───────►│ Emergency  │◄───────►│ Patient  │
│ (RCP)    │ 목록제공 │  (EMG)     │ 조회    │ (PAT)    │
└──────────┘         └─────┬──────┘         └──────────┘
                           │
   consultations(동기) / admission-request(비동기, Kafka)
                           │
                    OPD / IPT / ADM
```

- 동기: REST — 협진(`/consultations`), 병상 가용 조회(`GET`, IPT — 쓰기 없음)
- 비동기(Kafka): `order.created|confirmed|updated|cancelled|routed|validation.completed` 구독 + 입원요청 코레오그래피 `ADMISSION_REQUESTED → REGISTRATION_COMPLETED → BED_ASSIGNED`(오케스트레이터 없음, 상세: `flow.md` 7-1장)
- BFF: 경로가 `/api/emergency/*` 이어도 **데이터 Provider는 실제 소유자**로 문서화

## 5. 백엔드 계층 (권장)

```text
controller  →  RestController, 공통 Response
application →  UseCase / Service
domain      →  Entity, Domain Service
infrastructure → JPA/MyBatis, Feign/WebClient (Consumer), Oracle
```

패키지 루트: `kr.co.seoulit.his.emergencyservice`

도메인 패키지 제안: `triage`, `resource`, `care`, `channel`, `monitor`, `disposition`, `code`, `client`(외부 MSA)

## 6. Frontend 연동 (개발표준)

| 경로 | 역할 |
| --- | --- |
| `app/emergency/{story}/{pageType}/page.tsx` | 라우트 (리더 관리) |
| `components/emergency/` | 화면 |
| `features/emergency/{api,saga,slice,types,messages,utils}.ts` | 상태·API |

- 컴포넌트에서 axios 직접 호출 금지 → Saga만
- 메시지 코드: `EMG001` …
- 브랜치: `service/emergency` → Epic → Story

## 7. DB 설계 원칙

1. Oracle만 사용
2. PK `{table}_id` = **VARCHAR2(36) UUID** (MSA 공통). FK는 **서비스 내부만** 물리 FK; 타 서비스는 `reception_id`, `order_id`, `*_by_id` 논리 참조
3. 공통 컬럼: `created_at`, `updated_at`
4. 여부: `CHAR(1)` `'Y'/'N'`
5. 코드는 값만 저장, 코드명 스냅샷 금지
6. 삭제보다 상태/`use_yn`/`released_at` 이력 보존

상세 테이블: [models.md](./models.md)

## 8. API 설계 원칙

| 기능 | Method |
| --- | --- |
| 조회 | GET |
| 등록 | POST |
| 수정 | PUT / PATCH |
| 삭제 | 상태 변경 권장 |

응답:

```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {}
}
```

## 9. Open Question (아키텍처)

| ID | 질문 | 영향 |
| --- | --- | --- |
| Q-ROUTE-OWNER | LAB/SUR 전송 주체(기본=코어) | 직통 API 폐기 범위 |
| Q-ACK | ACKNOWLEDGED 사용 여부 | 간호 ACK |
| Q-SURGERY | SURGERY 코어 vs 채널문서만 | UC-ORD-10 |
| Q-EXAM | EXAM 코어 이관 시점 | 긴급오더 |
| Q-CHANNEL-REF | channelRef 필수 여부 | 협진·수술 추적 |
| Q-MAR-ORDERID | orderId 필수 강제 시점 | 처치/MAR |
| Q-RESULT-PATH | BFF vs LAB 직조회 | 결과 경로 |

## 10. 배포·인프라 (전사)

Docker 이미지 → Jenkins CI → Nginx(사내망). 서비스별 독립 배포.

메시지 브로커: **Kafka** 도입 — 서비스 간 비동기 이벤트(예: 입원요청 코레오그래피, GR2 `order.*` 이벤트 구독)에 사용. 중앙 오케스트레이터 서비스는 두지 않고, 각 서비스가 합의된 토픽만 구독/발행(코레오그래피). 우선순위상 환자등록·접수·진료 연결(REST) 골격을 먼저 구축한 뒤 착수.
