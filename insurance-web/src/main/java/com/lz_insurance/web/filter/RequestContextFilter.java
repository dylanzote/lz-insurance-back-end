package com.lz_insurance.web.filter;

import com.lz_insurance.core.context.RequestContext;
import com.lz_insurance.web.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Boundary filter that builds the {@link RequestContext} for every request and stows it in
 * {@link RequestContextHolder} for the {@code RequestContextArgumentResolver} to inject into
 * controllers. Also promotes the correlation id + client ip to MDC so every log line on the request
 * thread carries them, and echoes the correlation id back on {@value #CORRELATION_ID_HEADER}.
 *
 * <p>In M2 the identity fields (actorId/tenantId/sessionId) are left null — there is no JWT yet; M3
 * populates them from token claims here. Both the holder and the MDC keys are cleared in the finally
 * block, always, so nothing leaks onto a pooled thread.
 */
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String UNKNOWN = "unknown";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        String ipAddress = clientIp(request);

        RequestContext context = RequestContext.builder()
                .correlationId(correlationId)
                .ipAddress(ipAddress)
                .userAgent(headerOrDefault(request, "User-Agent"))
                .requestedAt(Instant.now())
                // actorId / tenantId / sessionId stay null until M3 wires JWT extraction
                .build();

        try {
            RequestContextHolder.set(context);
            MDC.put(MDC_CORRELATION_ID, correlationId);
            MDC.put(MDC_CLIENT_IP, ipAddress);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            chain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_CLIENT_IP);
        }
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        String provided = request.getHeader(CORRELATION_ID_HEADER);
        return (provided == null || provided.isBlank()) ? UUID.randomUUID().toString() : provided;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? UNKNOWN : remote;
    }

    private static String headerOrDefault(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? UNKNOWN : value;
    }
}
