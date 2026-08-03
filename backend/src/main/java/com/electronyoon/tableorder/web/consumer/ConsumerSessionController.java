package com.electronyoon.tableorder.web.consumer;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.order.OrderRepository;
import com.electronyoon.tableorder.domain.session.SessionStatus;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.web.dto.OrderResponse;
import com.electronyoon.tableorder.web.dto.SessionDetailResponse;
import com.electronyoon.tableorder.web.dto.TableSessionDto;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 확인 필요: "내 테이블 주문 내역"을 현재 OPEN 세션 기준으로 해석했다 (design.md에
 * CLOSED 세션 조회 요구사항이 명시돼 있지 않음). OPEN 세션이 없으면 404.
 */
@RestController
public class ConsumerSessionController {

    private final StoreTableRepository storeTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final OrderRepository orderRepository;

    public ConsumerSessionController(
            StoreTableRepository storeTableRepository,
            TableSessionRepository tableSessionRepository,
            OrderRepository orderRepository
    ) {
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/t/{qrToken}/session")
    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(@PathVariable String qrToken) {
        StoreTable table = storeTableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 테이블입니다."));

        TableSession session = tableSessionRepository.findByTableIdAndStatus(table.getId(), SessionStatus.OPEN)
                .orElseThrow(() -> ApiException.notFound("현재 진행 중인 세션이 없습니다."));

        var orders = orderRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(OrderResponse::from)
                .toList();

        return new SessionDetailResponse(TableSessionDto.from(session), orders);
    }
}
