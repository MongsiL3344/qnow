package io.github.mongsil3344.qnow.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_CACHED_REQUEST_BODY_BYTES = 8 * 1024;
    private static final int MAX_LOGGED_BODY_CHARS = 4 * 1024;
    private static final String MASKED_VALUE = "***";
    private static final String EMPTY_BODY = "(비어 있음)";
    private static final String LOG_SEPARATOR = "**************************************************";
    private static final String ANSI_ORANGE = "\u001B[38;5;208m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final Pattern SENSITIVE_JSON_VALUE_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|passwd|token|accessToken|refreshToken|csrfToken|verificationCode|secret|authorization|cookie|"
                    + "uploadUrl|thumbnailUploadUrl|pdfUrl)\"\\s*:\\s*)"
                    + "(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\s]+)"
    );
    private static final Pattern SENSITIVE_FORM_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:^|&)(?:password|passwd|token|access_token|refresh_token|csrf_token|verification_code|secret|"
                    + "authorization|cookie)=)[^&]*"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(
            request,
            MAX_CACHED_REQUEST_BODY_BYTES
        );
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(cachingRequest, cachingResponse);
        } finally {
            try {
                writeRequestLog(formatRequestLog(cachingRequest));
                writeResponseLog(formatResponseLog(cachingRequest, cachingResponse, startedAt));
            } finally {
                cachingResponse.copyBodyToResponse();
            }
        }
    }

    protected void writeRequestLog(String message) {
        log.info(colorizeRequestLog(message));
    }

    protected void writeResponseLog(String message) {
        log.info(colorizeResponseLog(message));
    }

    String colorizeRequestLog(String message) {
        return ANSI_ORANGE + message + ANSI_RESET;
    }

    String colorizeResponseLog(String message) {
        return ANSI_GREEN + message + ANSI_RESET;
    }

    private String formatRequestLog(ContentCachingRequestWrapper request) {
        boolean truncated = request.getContentLengthLong() > request.getContentAsByteArray().length;

        return """
                %s
                HTTP 요청
                메서드: %s
                경로: %s
                헤더: %s
                본문: %s
                %s
                """.formatted(
                LOG_SEPARATOR,
                request.getMethod(),
                request.getRequestURI(),
                formatRequestHeaders(request),
                formatBody(
                        request.getContentAsByteArray(),
                        request.getCharacterEncoding(),
                        request.getContentType(),
                        truncated
                ),
                LOG_SEPARATOR
        );
    }

    private String formatResponseLog(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long startedAt
    ) {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        return """

                %s
                HTTP 응답
                상태: %d
                메서드: %s
                경로: %s
                헤더: %s
                본문: %s
                소요 시간: %d ms
                %s""".formatted(
                LOG_SEPARATOR,
                response.getStatus(),
                request.getMethod(),
                request.getRequestURI(),
                formatResponseHeaders(response),
                formatBody(
                        response.getContentAsByteArray(),
                        response.getCharacterEncoding(),
                        response.getContentType(),
                        false
                ),
                durationMillis,
                LOG_SEPARATOR
        );
    }

    private String formatRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (request.getHeaderNames() == null) {
            return headers.toString();
        }

        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(
                name,
                formatHeaderValue(name, Collections.list(request.getHeaders(name)))
        ));

        return headers.toString();
    }

    private String formatResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        response.getHeaderNames().forEach(name -> headers.put(
                name,
                formatHeaderValue(name, response.getHeaders(name))
        ));

        return headers.toString();
    }

    private String formatHeaderValue(String name, Iterable<String> values) {
        if (isSensitiveHeader(name)) {
            return MASKED_VALUE;
        }

        StringBuilder joinedValues = new StringBuilder();

        for (String value : values) {
            if (!joinedValues.isEmpty()) {
                joinedValues.append(", ");
            }
            joinedValues.append(value);
        }

        return joinedValues.toString();
    }

    private boolean isSensitiveHeader(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);

        return normalizedName.equals("authorization")
                || normalizedName.equals("proxy-authorization")
                || normalizedName.equals("cookie")
                || normalizedName.equals("set-cookie")
                || normalizedName.contains("token")
                || normalizedName.contains("secret")
                || normalizedName.contains("api-key");
    }

    private String formatBody(byte[] content, String characterEncoding, String contentType, boolean truncated) {
        if (content.length == 0) {
            return EMPTY_BODY;
        }

        if (!isReadableContentType(contentType)) {
            return "[본문 생략: " + (contentType == null ? "알 수 없는 콘텐츠 타입" : contentType) + "]";
        }

        Charset charset = resolveCharset(characterEncoding);
        String body = new String(content, charset);

        if (body.length() > MAX_LOGGED_BODY_CHARS) {
            body = body.substring(0, MAX_LOGGED_BODY_CHARS);
            truncated = true;
        }

        String maskedBody = maskSensitiveBodyValues(body, contentType);

        return truncated ? maskedBody + "… [일부만 표시]" : maskedBody;
    }

    private boolean isReadableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);

        return normalizedContentType.startsWith("text/")
                || normalizedContentType.contains("json")
                || normalizedContentType.contains("xml")
                || normalizedContentType.contains("x-www-form-urlencoded")
                || normalizedContentType.contains("javascript");
    }

    private Charset resolveCharset(String characterEncoding) {
        if (characterEncoding == null) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(characterEncoding);
        } catch (RuntimeException ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    private String maskSensitiveBodyValues(String body, String contentType) {
        String maskedBody = replaceSensitiveValues(SENSITIVE_JSON_VALUE_PATTERN, body, "$1\"" + MASKED_VALUE + "\"");

        if (contentType.toLowerCase(Locale.ROOT).contains("x-www-form-urlencoded")) {
            return replaceSensitiveValues(SENSITIVE_FORM_VALUE_PATTERN, maskedBody, "$1" + MASKED_VALUE);
        }

        return maskedBody;
    }

    private String replaceSensitiveValues(Pattern pattern, String value, String replacement) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder maskedValue = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(maskedValue, replacement);
        }
        matcher.appendTail(maskedValue);

        return maskedValue.toString();
    }
}
