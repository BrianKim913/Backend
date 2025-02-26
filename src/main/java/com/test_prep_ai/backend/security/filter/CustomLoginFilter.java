package com.test_prep_ai.backend.security.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test_prep_ai.backend.security.JwtUtil;
import com.test_prep_ai.backend.security.dto.CustomUserDetails;
import com.test_prep_ai.backend.security.dto.TokenResponse;
import com.test_prep_ai.backend.response.DataResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        // 클라이언트 요청에서 username, password 추출
        String username;
        String password;

        Map<String, String> requestBody;
        try {
            requestBody = new ObjectMapper().readValue(request.getInputStream(), new TypeReference<Map<String, String>>(){});
        } catch (IOException e) {
            setBody(response, 400, "Failed to parse JSON request body");
            return null;
        }

        username = requestBody.get("username");
        password = requestBody.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            setBody(response, 400, "Username and password are required");
            return null;
        }

        // 스프링 시큐리티에서 username과 pw를 검증하기 위해 token에 담기
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                username, password
        );

        // token에 담은 정보를 검증하기 위해 AuthenticationManager로 전달
        return authenticationManager.authenticate(token);
    }

    // 로그인 성공 시
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {

        // 현재 요청의 유저는 누구인가
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal(); // 사용자의 주체 반환

        // 주체(principal)란. 일반적으로 사용자 이름이나 사용자 정보를 나타내는 객체
        Long id = customUserDetails.getId();
        String username = customUserDetails.getUsername();

        // token 만들기
        String token = jwtUtil.generateToken(id, username);

        // 응답 구성하기 (헤더, 바디)
        TokenResponse tokenDto = new TokenResponse(token);
        DataResponse<TokenResponse> tokenBody = DataResponse.of(tokenDto);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(tokenBody);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(jsonResponse);
    }

    // 로그인 실패 시
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        setBody(response, 401, "Unauthorized");
    }

    private void setBody(HttpServletResponse response, int code, String message) {
        try {
            response.setStatus(code);
            response.setContentType("application/json");
            String jsonResponse = "{\"code\": " + code + ", \"message\": " + message + "}";
            response.getWriter().write(jsonResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
