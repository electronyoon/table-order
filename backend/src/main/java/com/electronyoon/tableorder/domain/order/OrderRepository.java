package com.electronyoon.tableorder.domain.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // session/items를 JOIN FETCH로 함께 로딩한다 — open-in-view=false라서, 트랜잭션 밖(컨트롤러)의
    // DTO 매핑 시점에 LAZY 연관을 건드리면 LazyInitializationException이 난다 (idempotency 재시도
    // 응답 케이스에서 실제로 겪은 버그).
    @Query("SELECT o FROM Order o JOIN FETCH o.session JOIN FETCH o.items WHERE o.idempotencyKey = :idempotencyKey")
    Optional<Order> findByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.session JOIN FETCH o.items WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<Order> findAllByStatusOrderByCreatedAtAsc(@Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.session JOIN FETCH o.items ORDER BY o.createdAt ASC")
    List<Order> findAllOrderByCreatedAtAsc();

    @Query("SELECT o FROM Order o JOIN FETCH o.session JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    List<Order> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /** design.md §5: 상태 변경은 조건부 업데이트로 멱등 처리. 반환값이 0이면 이미 COMPLETED였던 것. */
    @Modifying
    @Query("UPDATE Order o SET o.status = 'COMPLETED' WHERE o.id = :id AND o.status = 'RECEIVED'")
    int completeIfReceived(@Param("id") Long id);

    /** ACK는 가게 단위(design.md §5) — 최초 1회만 반영, 이후 재요청은 항상 성공(멱등, 이미 표시됨). */
    @Modifying
    @Query("UPDATE Order o SET o.ackedAt = :ackedAt WHERE o.id = :id AND o.ackedAt IS NULL")
    int ackIfNotYet(@Param("id") Long id, @Param("ackedAt") OffsetDateTime ackedAt);
}
