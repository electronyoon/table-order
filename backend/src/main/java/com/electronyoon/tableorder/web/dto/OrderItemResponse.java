package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.order.OrderItem;

public record OrderItemResponse(
        Long id,
        Long orderId,
        Long menuId,
        String menuName,
        int unitPrice,
        int quantity,
        String note,
        String status
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getMenu().getId(),
                item.getMenuName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getNote(),
                item.getStatus().name()
        );
    }
}
