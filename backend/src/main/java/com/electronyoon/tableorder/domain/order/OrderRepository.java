package com.electronyoon.tableorder.domain.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

    List<Order> findAllByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
