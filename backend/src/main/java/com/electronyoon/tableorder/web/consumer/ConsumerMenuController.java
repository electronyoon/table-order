package com.electronyoon.tableorder.web.consumer;

import com.electronyoon.tableorder.common.ApiException;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.service.MenuService;
import com.electronyoon.tableorder.web.dto.MenuBoardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerMenuController {

    private final StoreTableRepository storeTableRepository;
    private final MenuService menuService;

    public ConsumerMenuController(StoreTableRepository storeTableRepository, MenuService menuService) {
        this.storeTableRepository = storeTableRepository;
        this.menuService = menuService;
    }

    @GetMapping("/t/{qrToken}")
    public MenuBoardResponse getMenuBoard(@PathVariable String qrToken) {
        storeTableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 테이블입니다."));
        return menuService.getMenuBoard();
    }
}
