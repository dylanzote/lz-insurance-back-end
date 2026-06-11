package com.lz_Insurance.security.core.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz_Insurance.core.exception.ErrorCode;
import com.lz_Insurance.web.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("Authentication failed: {}", authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ErrorCode.UNAUTHORIZED.getCode())
            .message("Authentication required. Please provide valid credentials.")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
