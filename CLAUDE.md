# table-order

QR 테이블 오더 시스템 모노레포 (backend / android / web). 설계 원본은 [docs/design.md](docs/design.md) (v0.2) — 도메인 규칙에 의문이 있으면 이 문서를 먼저 확인한다.

## 계약 우선 규칙

API를 변경할 때는 **[contracts/openapi.yaml](contracts/openapi.yaml)을 먼저 수정**하고, 영향받는 `backend`/`android`/`web`을 **같은 PR**에서 함께 갱신한다. 계약과 어긋나는 구현을 먼저 넣지 않는다.

## 도메인 핵심 규칙

- **자동 접수**: 주문은 생성 즉시 `RECEIVED`. 접수 버튼 없음. 상태는 `RECEIVED` / `COMPLETED` 2가지뿐.
- **품목 취소 = 당일 품절**: 품목 취소 시 `order_item.status = CANCELED`와 `menu.sold_out_date = 오늘 영업일`을 한 트랜잭션으로 처리한다. 품절 판정은 `sold_out_date == 오늘 영업일`이므로 날짜가 지나면 자동 해제된다 (별도 배치 불필요).
- **ACK는 가게 단위**: 태블릿/폰 중 한 기기가 대기열에 주문을 표시하면 ACK 완료. 상태 변경 API는 조건부 업데이트(`WHERE status='RECEIVED'`)로 멱등 처리한다.
- **idempotency_key**: 소비자 주문 생성은 클라이언트 생성 UUID(`idempotency_key`)로 중복 제출을 방지한다 (UNIQUE 제약, 재시도 시 기존 주문 반환).

## PR 규칙

- Conventional Commits (`feat:`, `fix:`, `chore:`, ...)
- 기능당 PR 1개
- PR 본문에 영향받는 프로젝트 체크박스 포함 (`- [ ] backend`, `- [ ] android`, `- [ ] web`, `- [ ] contracts`)

## 하위 프로젝트

- [backend/CLAUDE.md](backend/CLAUDE.md)
- [android/CLAUDE.md](android/CLAUDE.md)
- [web/CLAUDE.md](web/CLAUDE.md)
