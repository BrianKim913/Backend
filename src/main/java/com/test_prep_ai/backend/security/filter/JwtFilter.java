package com.test_prep_ai.backend.security.filter;

import com.test_prep_ai.backend.security.JwtUtil;
import com.test_prep_ai.backend.security.Session;
import com.test_prep_ai.backend.util.CONSTANT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 건너 뛰어야 하는 경로 건너뛰기
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 찾기
        String authorization = request.getHeader(CONSTANT.AUTHORIZATION_HEADER);

        // Authorization 헤더 검증 (Bearer로 시작하는지 검증)
        if (authorization == null || !authorization.startsWith(CONSTANT.BEARER_PREFIX)) {
            System.out.println("token null");
            setBody(response, 401, "Access token is null");
            return;
        }

        // Bearer 접두사 제거 후 순수 토큰 획득
        String token = authorization.split(" ")[1];

        // 토큰에서 정보 획득
        Long id = jwtUtil.getId(token);
        String username = jwtUtil.getUsername(token);

        // 매 요청마다 ContextHolder에 Authentication 추가 (왜냐하면 Stateless이니까)
        Session session = new Session(id, username);
        Authentication authToken = new UsernamePasswordAuthenticationToken(session, null, session.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken); // user session 생성

        // 다음 필터로 넘기기
        filterChain.doFilter(request, response);
    }

    private void setBody(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json");
        String jsonResponse = "{\"code\": " + code + ", \"message\": " + message + "}";
        response.getWriter().write(jsonResponse);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        List<String> skipURI = Arrays.asList("/login", "/auth/.*");

        return skipURI.stream().anyMatch(uri -> {
            Pattern pattern = Pattern.compile(uri);
            return pattern.matcher(request.getRequestURI()).matches();
        });
    }

}
