package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.common.BusinessDayCalculator;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.order.Order;
import com.electronyoon.tableorder.domain.order.OrderItem;
import com.electronyoon.tableorder.domain.order.OrderItemRepository;
import com.electronyoon.tableorder.domain.order.OrderRepository;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.order.OrderStatus;
import com.electronyoon.tableorder.domain.session.SessionStatus;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.web.dto.CreateOrderItemRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableSessionRepository tableSessionRepository;
    private final MenuRepository menuRepository;
    private final MenuService menuService;
    private final BusinessDayCalculator businessDayCalculator;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            TableSessionRepository tableSessionRepository,
            MenuRepository menuRepository,
            MenuService menuService,
            BusinessDayCalculator businessDayCalculator
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.menuRepository = menuRepository;
        this.menuService = menuService;
        this.businessDayCalculator = businessDayCalculator;
    }

    /**
     * 주문 생성 (자동 접수). idempotencyKey가 이미 존재하면 새로 만들지 않고 기존 주문을 반환한다
     * (design.md §6-1: "재시도 시 UNIQUE 충돌이면 기존 주문 반환").
     * 세션이 없으면 자동으로 연다 (design.md §1: QR 첫 주문 또는 사장님 구두 주문 입력 시 세션 생성).
     */
    @Transactional
    public OrderCreationResult createOrder(
            StoreTable table,
            OrderSource source,
            UUID idempotencyKey,
            List<CreateOrderItemRequest> itemRequests
    ) {
        var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new OrderCreationResult(existing.get(), false);
        }

        TableSession session = tableSessionRepository.findByTableIdAndStatus(table.getId(), SessionStatus.OPEN)
                .orElseGet(() -> tableSessionRepository.save(TableSession.open(table)));

        Order order = Order.create(session, source, idempotencyKey);
        for (CreateOrderItemRequest itemRequest : itemRequests) {
            Menu menu = menuRepository.findById(itemRequest.menuId())
                    .orElseThrow(() -> ApiException.notFound("존재하지 않는 메뉴입니다: " + itemRequest.menuId()));
            if (menuService.isSoldOut(menu)) {
                throw ApiException.conflict("MENU_SOLD_OUT", "품절된 메뉴입니다: " + menu.getName());
            }
            order.addItem(OrderItem.fromMenu(menu, itemRequest.quantity(), itemRequest.note()));
        }

        order = orderRepository.save(order);

        // 저장과 알림 분리(design.md §6-2)는 outbox_event/device 스키마가 도입되는 Phase 2에서 다룬다.
        return new OrderCreationResult(order, true);
    }

    /** 대기열 조회 (폴링 겸용). status가 null이면 전체 조회. */
    @Transactional(readOnly = true)
    public List<Order> listOrders(OrderStatus status) {
        return status == null
                ? orderRepository.findAllOrderByCreatedAtAsc()
                : orderRepository.findAllByStatusOrderByCreatedAtAsc(status);
    }

    /** ACK는 가게 단위 — 최초 1회만 기록, 이후 재요청은 항상 성공(멱등). */
    @Transactional
    public void ackOrder(Long orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("존재하지 않는 주문입니다."));
        orderRepository.ackIfNotYet(orderId, OffsetDateTime.now());
    }

    /** 조리완료(주문 단위, 조건부 업데이트). 이미 COMPLETED면 409. */
    @Transactional
    public void completeOrder(Long orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("존재하지 않는 주문입니다."));
        int updated = orderRepository.completeIfReceived(orderId);
        if (updated == 0) {
            throw ApiException.conflict("ALREADY_PROCESSED", "이미 처리된 주문입니다.");
        }
    }

    /**
     * 품목 취소: markSoldOut=false면 변심취소(취소만), true면 재고부족취소(취소+당일 품절, 한 트랜잭션).
     * 모든 품목이 CANCELED가 되면 주문도 자동 COMPLETED (design.md §3).
     */
    @Transactional
    public OrderItem cancelOrderItem(Long orderItemId, boolean markSoldOut) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 품목입니다."));

        item.cancel();

        if (markSoldOut) {
            item.getMenu().markSoldOut(businessDayCalculator.today());
        }

        Order order = item.getOrder();
        if (order.hasNoActiveItems()) {
            order.complete();
        }

        return item;
    }
}
