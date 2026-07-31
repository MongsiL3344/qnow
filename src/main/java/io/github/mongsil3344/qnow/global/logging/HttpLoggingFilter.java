package io.github.mongsil3344.qnow.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpLoggingFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = nanoTime();
        String requestId = createRequestId();
        boolean completedNormally = false;

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
            completedNormally = true;
        } finally {
            long durationMillis = Math.max(0, (nanoTime() - startedAt) / 1_000_000);
            int status = resolveStatus(response, completedNormally);

            writeAccessLog(requestId, request.getMethod(), request.getRequestURI(), status, durationMillis);
        }
    }

    protected String createRequestId() {
        return UUID.randomUUID().toString();
    }

    protected long nanoTime() {
        return System.nanoTime();
    }

    protected void writeAccessLog(
            String requestId,
            String method,
            String path,
            int status,
            long durationMillis
    ) {
        log.info(formatAccessLog(requestId, method, path, status, durationMillis));
    }

    String formatAccessLog(
            String requestId,
            String method,
            String path,
            int status,
            long durationMillis
    ) {
        return """

                RequestID: %s
                Path: %s
                Method: %s
                Status: %d
                DurationMs: %dms""".formatted(
                requestId,
                path,
                method,
                status,
                durationMillis
        );
    }

    private int resolveStatus(HttpServletResponse response, boolean completedNormally) {
        if (completedNormally || response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
            return response.getStatus();
        }

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
