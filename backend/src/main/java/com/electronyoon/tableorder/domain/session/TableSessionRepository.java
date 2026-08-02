package com.electronyoon.tableorder.domain.session;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findByTableIdAndStatus(Long tableId, SessionStatus status);
}
