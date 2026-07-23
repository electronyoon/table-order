package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.order.Order;
import com.electronyoon.tableorder.domain.order.OrderItem;
import com.electronyoon.tableorder.domain.order.OrderRepository;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.session.SessionStatus;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.web.dto.CreateOrderItemRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TableSessionRepository tableSessionRepository;
    private final MenuRepository menuRepository;
    private final MenuService menuService;

    public OrderService(
            OrderRepository orderRepository,
            TableSessionRepository tableSessionRepository,
            MenuRepository menuRepository,
            MenuService menuService
    ) {
        this.orderRepository = orderRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.menuRepository = menuRepository;
        this.menuService = menuService;
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
}
