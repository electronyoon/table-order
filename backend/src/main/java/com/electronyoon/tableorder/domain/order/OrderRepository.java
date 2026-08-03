package com.electronyoon.tableorder.domain.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // session/items를 JOIN FETCH로 함께 로딩한다 — open-in-view=false라서, 트랜잭션 밖(컨트롤러)의
    // DTO 매핑 시점에 LAZY 연관을 건드리면 LazyInitializationException이 난다 (idempotency 재시도
    // 응답 케이스에서 실제로 겪은 버그).
    @Query("SELECT o FROM Order o JOIN FETCH o.session JOIN FETCH o.items WHERE o.idempotencyKey = :idempotencyKey")
    Optional<Order> findByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    List<Order> findAllByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
