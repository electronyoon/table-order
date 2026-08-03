package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.order.Order;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        Long id,
        Long sessionId,
        String source,
        String status,
        UUID idempotencyKey,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSession().getId(),
                order.getSource().name(),
                order.getStatus().name(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
