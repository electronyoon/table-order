package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.service.SessionService;
import com.electronyoon.tableorder.web.dto.TableSessionDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSessionController {

    private final SessionService sessionService;

    public AdminSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/admin/sessions/{sessionId}/close")
    public TableSessionDto closeSession(@PathVariable Long sessionId) {
        TableSession session = sessionService.closeSession(sessionId);
        return TableSessionDto.from(session);
    }
}
