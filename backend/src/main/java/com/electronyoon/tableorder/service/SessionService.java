package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final TableSessionRepository tableSessionRepository;

    public SessionService(TableSessionRepository tableSessionRepository) {
        this.tableSessionRepository = tableSessionRepository;
    }

    /**
     * 세션 정산/비우기. 이미 CLOSED인 세션을 다시 닫아도 멱등하게 현재 상태를 반환한다
     * (계약에 이 케이스의 별도 에러가 정의돼 있지 않음).
     *
     * 확인 필요: design.md §4 "세션 정산 검증: sum(ACTIVE 품목 금액) == sum(PAID 금액) 불일치 시
     * 경고만"은 구현하지 않았다 — payment 관련 API/로직 자체가 이번 Phase 범위 밖이라(PG 연동은
     * design.md §9에서 이후 단계로 명시) 비교할 결제 데이터가 없다.
     */
    @Transactional
    public TableSession closeSession(Long sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 세션입니다."));
        if (session.getClosedAt() == null) {
            session.close();
        }
        return session;
    }
}
