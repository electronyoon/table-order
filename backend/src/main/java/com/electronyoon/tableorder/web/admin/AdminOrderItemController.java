package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.domain.order.OrderItem;
import com.electronyoon.tableorder.service.OrderService;
import com.electronyoon.tableorder.web.dto.CancelOrderItemRequest;
import com.electronyoon.tableorder.web.dto.OrderItemResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminOrderItemController {

    private final OrderService orderService;

    public AdminOrderItemController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * markSoldOut=false: 변심취소(취소만). markSoldOut=true: 재고부족취소(취소+당일 품절).
     * docs/design.md §3 확정 내용 — android에서는 일반 탭이 false, 길게 누르기가 true를 보낸다.
     */
    @PostMapping("/admin/order-items/{orderItemId}/cancel")
    public OrderItemResponse cancelOrderItem(
            @PathVariable Long orderItemId,
            @Valid @RequestBody CancelOrderItemRequest request
    ) {
        OrderItem item = orderService.cancelOrderItem(orderItemId, request.markSoldOut());
        return OrderItemResponse.from(item);
    }
}
