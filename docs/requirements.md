# 요구사항 정리 (UD2 응급관리)

근거: `HIS 응급관리.uml`, `UD2_응급관리_유스케이스_하위작업매핑.xlsx`, `개발표준가이드.md` 21장.

## 1. 목표

응급실에서 환자 내원부터 퇴실·전원까지 중증도 평가, 자원 배정, 진료·투여, 모니터링, 타 서비스 연계를 **단일 Bounded Context(EMG)** 로 제공한다.

## 2. Epic(패키지)별 유스케이스

분류: **단독형** / **연계형-Provider** / **연계형-Consumer** / **연계형-외부기관** / **신규·미정의**

### 2.1 상태평가 (Triage)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 1 | EMS 정보 조회 | 외부기관 | EMG | EMS(119) |
| 2 | KTAS 등급 분류 | 단독형 | EMG | - |
| 3 | KTAS 등급 재평가 | 단독형 | EMG | - |
| 4 | 조기 환자상태평가 | 단독형 | EMG | - |
| 5 | 감염병 격리 관리 | Consumer(부분) | EMG(+DUR 조회) | GR2/PHM |
| 6 | 패혈증·뇌졸중 위험도 스크리닝 | 단독형 | EMG | - |

### 2.2 자원 관리 (Resource)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 7 | 혼잡도 지표 조회 | 외부기관 | EMG | NEDIS |
| 8 | 구역·병상 배정 | 단독형 | EMG | - |
| 9 | 응급 의료기기 할당 | 단독형 | EMG | - |

### 2.3 응급 진료 (Care)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 10 | 응급환자 목록조회 | Provider | EMG | RCP가 Consumer |
| 11 | 응급 진료기록 입력 | 단독형 | EMG | - |
| 12 | 응급 처치 기록 | Consumer(참조) | EMG | GR2 `orderId` |
| 13 | 약물 투여 기록(MAR) | Consumer(참조) | EMG | GR2 `orderId` 필수 |
| 14 | CPR 타임라인 기록 | 단독형 | EMG | - |
| 15 | 동의 확인(체크) | **신규/미정의** | 미정 | Admin(문서양식) |
| 16 | 동의서 양식 조회(참조) | **신규/미정의** | 미정 | Admin |
| 17 | 동의 확인 유예 처리 | **신규/미정의** | 미정 | - |

> ERD 기준: 동의서는 시스템 범위 외로 확정되어 **테이블 없음**. API 설계 전 `개발표준가이드` 21.5(문서관리) 재확인 필요.

### 2.4 오더 (Order — EMG는 UI·채널, 원장은 GR2)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 18 | 구두처방 등록 | Consumer | GR2 | verbalYn=Y, encounterType=ER |
| 19 | 처방 변경 | Consumer | GR2 | 이력 보존 |
| 20 | 처방 취소 | Consumer | GR2 | 삭제 금지 |
| 21 | 사후 구두처방 확정 | Consumer | GR2 | VERBAL→ACTIVE |
| 22 | 검사·영상 긴급 오더 | Consumer | GR2→LAB | Q-EXAM Open |
| 23 | 검사·영상 결과 조회 | Consumer | LAB | Q-RESULT-PATH Open |
| 24 | 약제 조제 요청 | Consumer | GR2→PHM | PHM Provider 목록 미제출 |
| 25 | 조제 상태 조회 | Consumer | PHM | 동일 |
| 26 | 협진 요청 | Provider | EMG | OPD/IPT (중복설계 리스크) |
| 27 | 수술·시술 긴급 요청 | Consumer | GR2→SUR | Q-SURGERY Open |
| 28 | 당직의 호출 | Provider | EMG/ADM | ADM 역할 미확정 |

### 2.5 응급 모니터링 (Monitor)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 29 | 응급실 종합 현황판 | 단독형 | EMG | - |
| 30 | 장기체류 환자 알림 | 단독형 | EMG | - |
| 31 | 외부 병원 전원 정보 조회 | 외부기관 | EMG | NEDIS |

### 2.6 퇴실·전원 (Disposition)

| No | 유스케이스 | 분류 | Provider | 연계 |
| --- | --- | --- | --- | --- |
| 32 | 응급 퇴실 결정 | 단독형 | EMG | - |
| 33 | 응급 입원 요청 | Provider | EMG | IPT — **수신 API 부재** |
| 34 | 전원 소견서 작성 | Consumer | EMG(+투약조회) | GR2 |
| 35 | 구급차 이송 기록 | 단독형 | EMG | EMS 메타 기록 |

### 2.7 업무코드 (Codes)

| No | 유스케이스 | 분류 | Provider |
| --- | --- | --- | --- |
| 36~39 | 응급업무코드 조회/등록/수정/사용여부 변경 | 단독형 | EMG |

공유코드(KTAS 등급 마스터·진료과·약품·검사·진단 KCD)는 **admin-service** 공통코드 API 사용.

## 3. 기능 요구사항 요약

### FR-TRIAGE
- 119 사전정보 조회·저장
- KTAS 초기/재평가 이력 관리
- 활력징후·EWS 기록
- 격리 유형 등록·해제 (DUR은 조회만)
- 패혈증/뇌졸중 스크리닝 결과 저장

### FR-RESOURCE
- 구역·병상 마스터 및 배정/해제
- 의료기기 할당/반납
- 혼잡도(과밀화) 지표 산출·NEDIS 연계

### FR-CARE
- 접수 유입 응급환자 목록 제공(RCP Consumer)
- 진료기록·처치·MAR·CPR 타임라인 기록
- MAR/처치는 GR2 `orderId` 참조 (처방 이중기입 금지)

### FR-ORDER-CHANNEL
- 응급 UI에서 GR2 처방코어만 호출해 처방 생명주기 처리
- LAB/PHM/SUR **직통 오더 생성 금지**
- 협진·당직은 `/api/emergency/consultations`, `/on-call-pages`로 분리

### FR-MONITOR / FR-DISPOSITION
- 현황판·LOS 알림
- 퇴실 유형(귀가/입원/전원/사망/DAMA) 결정
- 입원요청·전원소견서·이송기록

### FR-CODE
- EMG 전용 코드그룹/코드값 CRUD (삭제는 `use_yn`)

## 4. 비기능 요구사항

| ID | 요구 |
| --- | --- |
| NFR-1 | REST 응답 포맷 `{ code, message, data }` 준수 |
| NFR-2 | 타 서비스 DB 직접 접근 금지, REST/Event만 |
| NFR-3 | 타 서비스 소유 데이터 스냅샷 저장 금지 (식별자만 보유) |
| NFR-4 | 목록 화면은 배치 조회 API로 N+1 방지 |
| NFR-5 | 삭제는 물리삭제보다 상태/`use_yn` 변경 |
| NFR-6 | Oracle 단일 DBMS, 컬럼 접미사·데이터타입 표준 준수 |
| NFR-7 | 메시지 코드 `EMG` + 3자리 (`EMG001` …) |
| NFR-8 | 서비스 간 순환 의존 금지 |

## 5. 우선 확인 리스크 (계약회의 전)

| # | 유스케이스 | 막힌 점 | 조치 |
| --- | --- | --- | --- |
| 1 | 응급 입원 요청 | IPT/RCP에 수신 POST API 없음 | 동기 API vs 이벤트 확정 |
| 2 | 당직의 호출 | ADM 알림 역할 불명확 | 발송 주체·채널 확정 |
| 3 | 협진 요청 | OPD `/referrals`와 중복 | 통합 vs 개별 유지 |
| 4 | 수술 긴급 요청 | Q-SURGERY 미확정 | GR2 경로 확정 후 착수 |
| 5 | 검사 긴급 오더 | Q-EXAM 이관 시점 | 과도기 운영 합의 |
| 6 | 조제 요청/상태 | PHM Provider 목록 미제출 | PHM 제출 요청 |
| 7 | 결과 조회 | Q-RESULT-PATH | BFF vs LAB 직조회 |
| 8 | 동의 확인/양식 | API·테이블 미정의 | 21.5 기준 Admin 연계 설계 |

## 6. 공통 하위작업 세트 (스토리당)

**백엔드:** BE-1 명세 → BE-2 Entity/DTO → BE-3 Controller → BE-4 로직 → BE-5 테스트 → BE-6 Swagger  
**프론트:** FE-1 types/api → FE-2 saga/slice → FE-3 components → FE-4 app 라우트(리더 요청)

연계형 추가: Provider `P-1/P-2`, Consumer `C-1/C-2/C-3`, 외부기관 `E-1/E-2`, 신규 `N-1/N-2`
