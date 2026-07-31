package io.github.mongsil3344.qnow.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpLoggingFilterTest {

    @Test
    void 요청이_끝나면_요약_정보를_항목별로_한_줄씩_기록한다() throws Exception {
        TestHttpLoggingFilter filter = new TestHttpLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.addHeader("Authorization", "Bearer request-secret");
        request.setQueryString("token=query-secret");
        request.setContent("""
                {"email":"owner@example.com","password":"plain-password"}
                """.getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(((MockHttpServletResponse) servletResponse).getHeader(HttpLoggingFilter.REQUEST_ID_HEADER))
                    .isEqualTo("test-request-id");
            servletRequest.getInputStream().readAllBytes();
            ((HttpServletResponse) servletResponse).setStatus(201);
            servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            servletResponse.getWriter().write("{\"token\":\"response-token\",\"message\":\"ok\"}");
        });

        assertThat(filter.accessLogs)
                .containsExactly("""

                        RequestID: test-request-id
                        Path: /login
                        Method: POST
                        Status: 201
                        DurationMs: 45ms""");
        assertThat(response.getHeader(HttpLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("test-request-id");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"token\":\"response-token\",\"message\":\"ok\"}"
        );
    }

    @Test
    void 처리되지_않은_예외는_상태를_500으로_기록한다() {
        TestHttpLoggingFilter filter = new TestHttpLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/fail");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new ServletException("test failure");
        })).isInstanceOf(ServletException.class);

        assertThat(filter.accessLogs)
                .containsExactly("""

                        RequestID: test-request-id
                        Path: /fail
                        Method: GET
                        Status: 500
                        DurationMs: 45ms""");
    }

    private static final class TestHttpLoggingFilter extends HttpLoggingFilter {

        private static final long STARTED_AT_NANOS = 100_000_000;
        private static final long FINISHED_AT_NANOS = 145_000_000;

        private final List<String> accessLogs = new ArrayList<>();
        private int nanoTimeCallCount;

        @Override
        protected String createRequestId() {
            return "test-request-id";
        }

        @Override
        protected long nanoTime() {
            return nanoTimeCallCount++ == 0 ? STARTED_AT_NANOS : FINISHED_AT_NANOS;
        }

        @Override
        protected void writeAccessLog(
                String requestId,
                String method,
                String path,
                int status,
                long durationMillis
        ) {
            accessLogs.add(formatAccessLog(requestId, method, path, status, durationMillis));
        }
    }
}
