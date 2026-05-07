package com.sentinelgrid.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sentinelgrid.config.SentinelGridProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PayloadSizeFilter extends OncePerRequestFilter {

    private final long maxPayloadBytes;

    public PayloadSizeFilter(SentinelGridProperties properties) {
        this.maxPayloadBytes = properties.getSecurity().getMaxPayloadBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        int contentLength = request.getContentLength();
        if (contentLength > maxPayloadBytes) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.getWriter().write("{\"error\":\"Payload too large\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
