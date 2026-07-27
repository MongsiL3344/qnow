package io.github.mongsil3344.qnow.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

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
    void 요청과_응답_로그에_서로_다른_ANSI_색상을_적용한다() {
        HttpLoggingFilter filter = new HttpLoggingFilter();

        assertThat(filter.colorizeRequestLog("요청"))
                .isEqualTo("\u001B[38;5;208m요청\u001B[0m");
        assertThat(filter.colorizeResponseLog("응답"))
                .isEqualTo("\u001B[32m응답\u001B[0m");
    }

    @Test
    void 민감한_값을_가리면서_요청과_응답을_기록한다() throws Exception {
        TestHttpLoggingFilter filter = new TestHttpLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.addHeader("Authorization", "Bearer request-secret");
        request.addHeader("Cookie", "JSESSIONID=session-secret");
        request.addHeader("X-CSRF-TOKEN", "csrf-secret");
        request.setContent("""
                {"email":"owner@example.com","password":"plain-password","verificationCode":"123456"}
                """.getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ((HttpServletResponse) servletResponse).setHeader("Set-Cookie", "JSESSIONID=response-secret");
            servletResponse.getWriter().write("{\"token\":\"response-token\",\"message\":\"ok\"}");
        });

        assertThat(filter.requestLogs).singleElement().satisfies(log -> {
            assertThat(log).startsWith("**************************************************\n");
            assertThat(log).doesNotContain("↓");
            assertThat(log).contains(
                    "HTTP 요청",
                    "메서드: POST",
                    "경로: /login",
                    "Authorization=***",
                    "Cookie=***",
                    "X-CSRF-TOKEN=***",
                    "\"email\":\"owner@example.com\"",
                    "\"password\":\"***\"",
                    "\"verificationCode\":\"***\""
            );
            assertThat(log).doesNotContain(
                "plain-password",
                "123456",
                "request-secret",
                "session-secret",
                "csrf-secret"
            );
        });
        assertThat(filter.responseLogs).singleElement().satisfies(log -> {
            assertThat(log).startsWith("\n**************************************************\nHTTP 응답\n");
            assertThat(log).endsWith("\n**************************************************");
            assertThat(log).contains(
                    "HTTP 응답",
                    "상태: 200",
                    "메서드: POST",
                    "경로: /login",
                    "Set-Cookie=***",
                    "\"token\":\"***\"",
                    "\"message\":\"ok\"",
                    "소요 시간:"
            );
            assertThat(log).doesNotContain("response-secret", "response-token");
        });
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"token\":\"response-token\",\"message\":\"ok\"}"
        );
    }

    @Test
    void 바이너리_본문은_생략하고_응답은_보존한다() throws Exception {
        TestHttpLoggingFilter filter = new TestHttpLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/presentations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] binaryContent = {0, 1, 2, 3};
        request.setContentType(MediaType.APPLICATION_PDF_VALUE);
        request.setContent(binaryContent);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletResponse.setContentType(MediaType.APPLICATION_PDF_VALUE);
            servletResponse.getOutputStream().write(binaryContent);
        });

        assertThat(filter.requestLogs).singleElement().asString().contains("본문: [본문 생략: application/pdf]");
        assertThat(filter.responseLogs).singleElement().asString().contains("본문: [본문 생략: application/pdf]");
        assertThat(response.getContentAsByteArray()).containsExactly(binaryContent);
    }

    private static final class TestHttpLoggingFilter extends HttpLoggingFilter {

        private final List<String> requestLogs = new ArrayList<>();
        private final List<String> responseLogs = new ArrayList<>();

        @Override
        protected void writeRequestLog(String message) {
            requestLogs.add(message);
        }

        @Override
        protected void writeResponseLog(String message) {
            responseLogs.add(message);
        }
    }
}
