package com.electronyoon.tableorder.web.consumer;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.service.OrderCreationResult;
import com.electronyoon.tableorder.service.OrderService;
import com.electronyoon.tableorder.web.dto.CreateOrderRequest;
import com.electronyoon.tableorder.web.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerOrderController {

    private final StoreTableRepository storeTableRepository;
    private final OrderService orderService;

    public ConsumerOrderController(StoreTableRepository storeTableRepository, OrderService orderService) {
        this.storeTableRepository = storeTableRepository;
        this.orderService = orderService;
    }

    @PostMapping("/t/{qrToken}/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable String qrToken,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        StoreTable table = storeTableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 테이블입니다."));

        OrderCreationResult result = orderService.createOrder(
                table, OrderSource.QR, request.idempotencyKey(), request.items());

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OrderResponse.from(result.order()));
    }
}
