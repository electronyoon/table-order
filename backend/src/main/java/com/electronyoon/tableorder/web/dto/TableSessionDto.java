package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.session.TableSession;
import java.time.OffsetDateTime;

public record TableSessionDto(
        Long id,
        Long tableId,
        String status,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt
) {

    public static TableSessionDto from(TableSession session) {
        return new TableSessionDto(
                session.getId(),
                session.getTable().getId(),
                session.getStatus().name(),
                session.getOpenedAt(),
                session.getClosedAt()
        );
    }
}
