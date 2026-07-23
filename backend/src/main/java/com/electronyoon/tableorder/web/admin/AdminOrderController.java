package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.order.OrderStatus;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.service.OrderCreationResult;
import com.electronyoon.tableorder.service.OrderService;
import com.electronyoon.tableorder.web.dto.CreateOrderRequest;
import com.electronyoon.tableorder.web.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminOrderController {

    private final OrderService orderService;
    private final StoreTableRepository storeTableRepository;

    public AdminOrderController(OrderService orderService, StoreTableRepository storeTableRepository) {
        this.orderService = orderService;
        this.storeTableRepository = storeTableRepository;
    }

    @GetMapping("/admin/orders")
    public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.listOrders(status).stream().map(OrderResponse::from).toList();
    }

    /** 사장님 직접 입력(source=COUNTER). CreateOrderRequest.tableId 필수 (확인 필요 — 계약에 이 필드가 없어서 이번 PR에서 추가함). */
    @PostMapping("/admin/orders")
    public ResponseEntity<OrderResponse> createCounterOrder(@Valid @RequestBody CreateOrderRequest request) {
        if (request.tableId() == null) {
            throw ApiException.conflict("TABLE_ID_REQUIRED", "COUNTER 주문은 tableId가 필요합니다.");
        }
        StoreTable table = storeTableRepository.findById(request.tableId())
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 테이블입니다."));

        OrderCreationResult result = orderService.createOrder(
                table, OrderSource.COUNTER, request.idempotencyKey(), request.items());

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OrderResponse.from(result.order()));
    }

    @PostMapping("/admin/orders/{orderId}/ack")
    public ResponseEntity<Void> ackOrder(@PathVariable Long orderId) {
        orderService.ackOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/orders/{orderId}/complete")
    public ResponseEntity<Void> completeOrder(@PathVariable Long orderId) {
        orderService.completeOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
