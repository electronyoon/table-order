package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.domain.order.Order;

/** idempotencyKey 재시도로 기존 주문을 반환한 경우 created=false (컨트롤러가 200 vs 201을 결정). */
public record OrderCreationResult(Order order, boolean created) {
}
