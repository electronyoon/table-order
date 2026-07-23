-- ACK(가게 단위, design.md §5/§6)를 order 단위로 최소 구현하기 위한 컬럼.
-- outbox_event.status(NEW/SENT/ACKED)는 FCM dispatch 스케줄러가 생기는 다음 단계에서
-- 실제로 쓰이게 되고, 지금은 order에 "한 번이라도 화면에 표시됐는지"만 조건부 업데이트로
-- 멱등하게 기록한다.
ALTER TABLE orders ADD COLUMN acked_at TIMESTAMPTZ;
