package com.electronyoon.tableorder.web.dto;

import java.util.List;

public record SessionDetailResponse(TableSessionDto session, List<OrderResponse> orders) {
}
