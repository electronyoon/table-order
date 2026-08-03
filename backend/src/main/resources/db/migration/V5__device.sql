-- 기기 등록(POST /admin/devices)용 테이블.
-- design v0.3(#9)에서 device를 Phase 2로 분리하며 V1에서 제거했으므로,
-- 이 기능을 쓰는 이 PR에서 별도 마이그레이션으로 도입한다.
-- (outbox_event 및 acked_by_device_id 참조는 FCM dispatch가 붙는 Phase 2에서 추가)
CREATE TABLE device (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(10) NOT NULL CHECK (role IN ('PRIMARY', 'BACKUP')),
    fcm_token     VARCHAR(255) NOT NULL,
    last_seen_at  TIMESTAMPTZ
);
