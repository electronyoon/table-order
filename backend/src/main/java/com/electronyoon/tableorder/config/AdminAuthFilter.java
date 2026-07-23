package com.electronyoon.tableorder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * contracts/openapi.yaml의 관리자 API 인증(`Authorization: Bearer <static-token>`)을
 * 검사하는 최소 구현. "초기 버전, 사용자별 로그인 없음"(design.md) — Spring Security 없이
 * 단순 static token 비교로 처리한다.
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public AdminAuthFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;

        if (token == null || !token.equals(expectedToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"관리자 인증이 필요합니다.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
