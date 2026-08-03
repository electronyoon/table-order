-- 바쁨 모드 on/off (design.md §9 Phase 5 언급, 구체 스키마는 명시되지 않아 최소 구현).
-- 단일 행만 사용하는 설정 테이블. 재시작 후에도 유지되어야 하므로 DB에 저장한다.
CREATE TABLE busy_mode (
    id          BIGSERIAL PRIMARY KEY,
    enabled     BOOLEAN NOT NULL DEFAULT false,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO busy_mode (enabled) VALUES (false);
