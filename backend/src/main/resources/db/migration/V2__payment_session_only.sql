-- design v0.3 (#9): 선불/PG 스코프 제외 → Payment는 세션 전속.
-- order_id / XOR CHECK / pg_* 를 제거하고 session_id 를 NOT NULL 로 승격한다.
-- (V1은 이미 배포되어 수정 불가하므로 신규 마이그레이션으로 처리)

ALTER TABLE payment DROP CONSTRAINT ck_payment_session_xor_order;

ALTER TABLE payment DROP COLUMN order_id;
ALTER TABLE payment DROP COLUMN pg_provider;
ALTER TABLE payment DROP COLUMN pg_tid;

ALTER TABLE payment ALTER COLUMN session_id SET NOT NULL;
