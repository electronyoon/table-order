# 테이블 오더 시스템 설계 v0.3

Spring Boot(Java) + PostgreSQL + Android(Kotlin) 주방 앱 + 소비자 PWA 기준.

v0.3 변경: 선불(QR 즉시결제)/PG 연동을 스코프에서 제외. 결제는 **카운터 후불 정산만** 지원 → `Payment`는 세션 전속(`order_id`·XOR CHECK·`pg_*` 제거). 선불은 도입 시 결제 모델을 재설계한다(§4).
v0.2 변경: 자동 접수, 품목 취소 = 당일 품절 처리, 대기열 2버튼(취소/조리완료), 사장님 폰 = 백업 수신 기기.

---

## 1. 핵심 개념: 테이블 세션

주문(Order) 위에 **테이블 세션(TableSession)** 을 둔다. 손님 한 팀이 앉아서 나갈 때까지가 하나의 세션이고, 그 안에서 여러 번 주문(추가 주문)이 발생한다.

- 결제는 **카운터 후불**만 지원한다. **세션 단위**로 정산 → `Payment.session_id` (세션 1건에 결제 1건).
- 선불(QR 즉시결제)/PG 연동은 v0.3에서 스코프 제외. 도입 시 결제가 **주문 단위**로 바뀌므로 `Payment`에 주문 단위 결제를 다시 설계한다(§4). 현재 스키마에는 넣지 않는다.

세션 시작: QR 첫 주문 시 자동 생성, 또는 사장님이 태블릿에서 구두 주문 입력 시 생성.
세션 종료: 사장님이 태블릿에서 "정산/비우기" 처리.

---

## 2. ERD

```
store_table ──< table_session ──< orders ──< order_item >── menu >── menu_category
                     │                │
                     └──< payment >───┘     outbox_event*   device*(수신 기기)
```
\* `outbox_event` / `device`는 알림 발송(§5, §6-2) 설계를 위한 계획된(planned) 테이블이다. Phase 1(현재)에는 스키마·엔티티에 포함하지 않으며, Phase 2 안드로이드 앱 알림 구현 시 별도 마이그레이션으로 도입한다(§9).

### store_table
| 컬럼 | 설명 |
|---|---|
| id | PK |
| label | "3번", "야외1" 등 |
| qr_token | QR에 심을 랜덤 토큰 |

### table_session
| 컬럼 | 설명 |
|---|---|
| id, table_id | |
| status | `OPEN` / `CLOSED` |
| opened_at, closed_at | |

제약: 테이블당 OPEN 세션 1개 (partial unique index `WHERE status='OPEN'`).

### menu_category / menu
| 컬럼 | 설명 |
|---|---|
| id, category_id, name, price, sort_order | |
| is_self_service | 바쁨 모드에서 셀프 안내 대상 |
| sold_out_date | **DATE, nullable.** "품절 여부"가 아니라 "품절된 영업일"을 저장 |

**당일 품절 자동 해제 트릭**: 품절 판정은 `sold_out_date == 오늘 영업일`. 날짜가 지나면 조건이 자동으로 거짓이 되므로 해제 배치/크론이 필요 없다.
- 영업일 계산: 새벽 영업 대비 영업일 시작 시각(기본 06:00)을 두고, 그 이전이면 전날을 영업일로 간주.
- 사장님이 착오로 품절 처리한 경우를 위해 메뉴판 화면에서 품절 배지 탭 → "품절 해제"만 제공 (러닝커브 없는 단일 액션).

### orders
| 컬럼 | 설명 |
|---|---|
| id, session_id, created_at | |
| source | `QR` / `COUNTER` |
| status | `RECEIVED` / `COMPLETED` — 접수 버튼 없음, 생성 즉시 자동 접수 |
| idempotency_key | UNIQUE. 클라이언트 생성 UUID |

### order_item
| 컬럼 | 설명 |
|---|---|
| order_id, menu_id, quantity, note | |
| menu_name, unit_price | 주문 시점 스냅샷 |
| status | `ACTIVE` / `CANCELED` — **취소는 품목 단위** |

### payment — 세션 후불 정산 (session_id NOT NULL, method, status, amount)

### device *(planned — Phase 2 안드로이드 앱 알림 구현 시 도입, 현재 스키마에 없음)*
| 컬럼 | 설명 |
|---|---|
| id, name | "주방 태블릿", "엄마 폰" |
| role | `PRIMARY` / `BACKUP` |
| fcm_token, last_seen_at | |

### outbox_event *(planned — Phase 2 안드로이드 앱 알림 구현 시 도입, 현재 스키마에 없음)*
| 컬럼 | 설명 |
|---|---|
| id, type, payload, created_at | |
| status | `NEW` / `SENT` / `ACKED` — ACK는 **가게 단위** (어느 기기든 하나가 확인하면 완료) |
| acked_by_device_id | 어떤 기기가 확인했는지 기록 |

---

## 3. 주문 흐름 (자동 접수)

```
주문 생성 ──(자동 접수)──> RECEIVED (대기열 노출 + 알림음)
                              │
                              ├─ [조리완료] ──> COMPLETED (대기열에서 제거)
                              │
                              └─ [품목 취소] ─> 확인 모달 ─> item CANCELED
                                                              + 해당 메뉴 당일 품절
```

### 대기열 화면 규칙
- 버튼은 **취소 / 조리완료** 두 개뿐. 접수 단계 없음.
- 주문 카드에는 품목별로 취소 버튼. **조리완료는 주문 단위** (남은 ACTIVE 품목 전체 완료 처리).
- 모든 품목이 취소되면 주문도 자동 COMPLETED (빈 카드 제거).

### 품목 취소: 변심취소 vs 재고부족취소 (확정)
취소 제스처는 2가지이며, **둘 다 확인 모달이 필수**다 (실수 방지):

1. **일반 탭 = 변심취소**: 품목 [취소] 탭 → 모달: **"'제육볶음'을 취소할까요?"** [취소하기] / [돌아가기]
   → `order_item.status = CANCELED`만 처리. 품절 처리 없음.
2. **길게 누르기 = 재고부족취소**: 품목 [취소]를 길게 누름 → 모달: **"'제육볶음'을 취소하고 오늘 하루 품절 처리할까요?"** [품절 처리하고 취소] / [돌아가기]
   → 확정 시 한 트랜잭션으로:
     - `order_item.status = CANCELED`
     - `menu.sold_out_date = 오늘 영업일`
   → 부수 효과:
     - PWA 메뉴판에서 즉시 "품절" 표시, 주문 불가
     - 품절 시점에 대기열에 이미 들어와 있던 **다른 주문의 같은 메뉴 품목을 노란색으로 하이라이트** → 사장님이 그 품목들도 취소할지 개별 판단 (자동 연쇄 취소는 하지 않음 — 마지막 1인분이 남아있을 수 있으므로)
3. 취소 사유 입력 없음.

일반 탭(변심취소)이 더 빈번하고, 재고부족취소는 메뉴 전체 가용성에 영향을 주는 더 무거운 액션이라 길게 누르기 뒤에 둔다. 오품절 시 복구는 메뉴판에서 품절 배지 탭 → 해제로 가능 (기존과 동일).

---

## 4. 결제 상태 (주문 상태와 분리)

- 카운터 후불만: 세션 CLOSE 시 `Payment(PAID)`를 **세션 단위로 1건** 기록.
- 세션 정산 검증: `sum(ACTIVE 품목 금액) == sum(PAID 금액)` 불일치 시 경고만.
- **(스코프 밖) 선불/PG**: 도입 시 "결제 완료 후에만 주문 생성"으로 처리하고, 그 시점에 `Payment`의 주문 단위 결제(별도 컬럼 또는 테이블)와 **자동 접수 후 결제 실패 취소**를 상세 설계한다. 지금 스키마에는 반영하지 않는다.

---

## 5. 알림/수신 구조: 태블릿 + 사장님 폰 (2-device)

같은 안드로이드 앱을 두 기기에 설치하고 역할만 다르게 설정.

| | 주방 태블릿 (PRIMARY) | 사장님 폰 (BACKUP) |
|---|---|---|
| 대기열 표시 | 항상 (메인 화면) | 앱 열면 동일 화면 |
| FCM 수신 | 즉시 알림음 | 즉시 수신하되 **조용히** |
| 알림음 | 주문 즉시 | **미확인 60초 경과 시에만** 에스컬레이션 알람 |
| 폴링 | 15~30초 | 앱 포그라운드일 때만 |

- **ACK는 가게 단위**: 태블릿이든 폰이든 대기열에 주문이 "화면에 표시"되면 ACK. 태블릿이 정상이면 폰은 영원히 조용하다.
- 폰의 가치: ① FCM/와이파이 문제로 태블릿이 못 받을 때 60초 내 백업 알람 ② **가게 인터넷 전체가 죽었을 때** 폰은 LTE로 서버에 붙으므로 주문 수신·처리 가능 (손님 QR 주문은 손님 휴대폰 데이터로 서버에 도달하므로 가게 인터넷과 무관하게 계속 들어온다 — 이 시나리오를 폰이 유일하게 커버)
- 폰 설정 필수사항: FCM high-priority 메시지 사용, 배터리 최적화 제외 등록, 알림 채널 무음/알람 분리.
- 처리 충돌: 두 기기가 같은 주문을 동시에 조작할 수 있으므로 상태 변경 API는 조건부 업데이트(`WHERE status='RECEIVED'`)로 멱등 처리, 밀린 쪽에는 "이미 처리됨" 표시.

---

## 6. 주문 유실 방지 (v0.1 유지 + 보강)

1. **생성**: PWA가 UUID 생성 → POST, 200 전까지 완료 화면 없음, 재시도 시 UNIQUE 충돌이면 기존 주문 반환.
2. **저장과 알림 분리** *(planned, Phase 2)*: 주문 저장 + outbox_event를 한 트랜잭션 → 스케줄러가 FCM 발송(모든 device 대상).
3. **수신 보장**: 기기 표시 시 ACK(가게 단위) + 폴링 백업 + 60초 미ACK 시 폰 에스컬레이션.
4. **복구**: 앱 시작/재접속 시 `status=RECEIVED` 주문 전체 재조회 (ACK 여부와 무관하게 대기열은 항상 서버가 원본).

---

## 7. 장애 시나리오

| 시나리오 | 대응 |
|---|---|
| 주문 버튼 연타 | idempotency_key UNIQUE |
| 주문 직후 손님 와이파이 끊김 | 200 수신 전 재시도 |
| FCM 유실 | 폴링 + 폰 에스컬레이션 |
| 태블릿 와이파이 순단/재부팅 | 재접속 시 RECEIVED 전체 재조회, 부팅 자동시작 |
| 가게 인터넷 전면 장애 | 사장님 폰 LTE로 수신·처리 지속 |
| 두 기기 동시 조작 | 조건부 업데이트로 한쪽만 성공 |
| 품절 직후 들어온 주문 | 주문 생성 시 sold_out_date 검증 → 409 |
| 품절 시점에 대기열에 있던 같은 메뉴 | 하이라이트만, 자동 취소 없음 |
| 오품절 (변심 취소를 품절 처리) | 메뉴판에서 품절 배지 탭 → 해제 |
| 메뉴 가격 변경 | order_item 스냅샷 |

---

## 8. API 스케치

```
# 소비자 (PWA)
GET  /t/{qr_token}                     # 메뉴판 (품절 반영)
POST /t/{qr_token}/orders              # 주문 생성 → 즉시 RECEIVED
GET  /t/{qr_token}/session             # 내 테이블 주문 내역

# 주방 (Android, 태블릿/폰 공용)
GET   /admin/orders?status=RECEIVED    # 대기열 (폴링 겸용)
POST  /admin/orders/{id}/ack           # 표시 확인 (가게 단위)
POST  /admin/orders/{id}/complete      # 조리완료 (조건부 업데이트)
POST  /admin/order-items/{id}/cancel   # 품목 취소 + 당일 품절 (한 트랜잭션)
POST  /admin/menus/{id}/restore        # 품절 해제
POST  /admin/orders                    # 사장님 직접 입력 (source=COUNTER)
POST  /admin/sessions/{id}/close       # 정산
POST  /admin/busy-mode                 # 바쁨 모드 on/off
POST  /admin/devices                   # 기기 등록 (FCM 토큰, role) — planned, Phase 2
```

---

## 9. 구현 순서

1. 스키마 + Spring Boot 도메인/API (COUNTER 입력 → 대기열 → 완료/취소·품절까지)
2. Android 앱: 대기열 + 2버튼 + 취소 모달 + 알림음 + 기기 role (`device`/`outbox_event` 스키마 도입) → **실사용 시작**
3. 사장님 폰 BACKUP 모드 (에스컬레이션 알람)
4. PWA 메뉴판 + QR 주문 (품절 표시 포함)
5. 바쁨 모드 / 정산 화면
6. (스코프 밖, 향후) 선불/PG 연동 — 도입 시 결제 모델 재설계 (§4)
