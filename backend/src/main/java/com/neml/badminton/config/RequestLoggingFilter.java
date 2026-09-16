package com.neml.badminton.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String ACTOR_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".actor";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = correlationId(request.getHeader(CORRELATION_HEADER));
        long startedAt = System.nanoTime();
        MDC.put("traceId", traceId);
        response.setHeader(CORRELATION_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            log.error("Unhandled request failure method={} path={} actor={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), actor(request), elapsedMillis(startedAt), exception);
            throw exception;
        } finally {
            int status = response.getStatus();
            if ("/api/health".equals(request.getRequestURI()) && status < 400) {
                log.debug("Request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, elapsedMillis(startedAt));
            } else {
                log.info("Request completed method={} path={} status={} actor={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, actor(request), elapsedMillis(startedAt));
            }
            MDC.remove("traceId");
        }
    }

    private String correlationId(String supplied) {
        return supplied != null && SAFE_CORRELATION_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }

    private String actor(HttpServletRequest request) {
        Object actor = request.getAttribute(ACTOR_ATTRIBUTE);
        return actor == null ? "anonymous" : actor.toString();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
