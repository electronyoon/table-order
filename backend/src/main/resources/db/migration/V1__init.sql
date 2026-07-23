-- table-order 초기 스키마
-- 설계 원본: docs/design.md §2 ERD

CREATE TABLE store_table (
    id        BIGSERIAL PRIMARY KEY,
    label     VARCHAR(50) NOT NULL,
    qr_token  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE table_session (
    id         BIGSERIAL PRIMARY KEY,
    table_id   BIGINT NOT NULL REFERENCES store_table (id),
    status     VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at  TIMESTAMPTZ
);

-- 테이블당 OPEN 세션은 최대 1개
CREATE UNIQUE INDEX ux_table_session_open_per_table
    ON table_session (table_id)
    WHERE status = 'OPEN';

CREATE TABLE menu_category (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE menu (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT NOT NULL REFERENCES menu_category (id),
    name             VARCHAR(100) NOT NULL,
    price            INT NOT NULL,
    sort_order       INT NOT NULL DEFAULT 0,
    is_self_service  BOOLEAN NOT NULL DEFAULT false,
    -- 품절 여부가 아니라 "품절된 영업일"을 저장한다 (오늘 영업일과 일치하면 품절)
    sold_out_date    DATE
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT NOT NULL REFERENCES table_session (id),
    source           VARCHAR(10) NOT NULL CHECK (source IN ('QR', 'COUNTER')),
    status           VARCHAR(10) NOT NULL CHECK (status IN ('RECEIVED', 'COMPLETED')),
    idempotency_key  UUID NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_item (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders (id),
    menu_id     BIGINT NOT NULL REFERENCES menu (id),
    -- 주문 시점 스냅샷 (메뉴 가격 변경과 무관)
    menu_name   VARCHAR(100) NOT NULL,
    unit_price  INT NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0),
    note        VARCHAR(200),
    status      VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELED'))
);

CREATE TABLE payment (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT REFERENCES table_session (id),
    order_id    BIGINT REFERENCES orders (id),
    method      VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    amount      INT NOT NULL,
    -- PG 연동 예약 컬럼 (v0.1과 동일, 현재 미사용)
    pg_provider VARCHAR(50),
    pg_tid      VARCHAR(100),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_session_xor_order CHECK (
        (session_id IS NOT NULL AND order_id IS NULL) OR
        (session_id IS NULL AND order_id IS NOT NULL)
    )
);

CREATE TABLE device (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(10) NOT NULL CHECK (role IN ('PRIMARY', 'BACKUP')),
    fcm_token     VARCHAR(255) NOT NULL,
    last_seen_at  TIMESTAMPTZ
);

CREATE TABLE outbox_event (
    id                  BIGSERIAL PRIMARY KEY,
    type                VARCHAR(50) NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(10) NOT NULL CHECK (status IN ('NEW', 'SENT', 'ACKED')),
    -- ACK는 가게 단위: 어느 기기든 하나가 확인하면 완료
    acked_by_device_id  BIGINT REFERENCES device (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
