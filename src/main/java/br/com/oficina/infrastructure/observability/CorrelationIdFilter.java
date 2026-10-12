package br.com.oficina.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_REQUEST_URI = "requestUri";
    private static final String MDC_STATUS_CODE = "statusCode";
    private static final String MDC_ELAPSED_MS = "elapsedMs";

    private static final int MAX_CORRELATION_ID_LENGTH = 128;
    private static final Pattern SAFE_CORRELATION_ID =
        Pattern.compile("^[A-Za-z0-9._:-]{1," + MAX_CORRELATION_ID_LENGTH + "}$");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String correlationId = resolveCorrelationId(request);

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put(MDC_STATUS_CODE, String.valueOf(response.getStatus()));
            MDC.put(MDC_ELAPSED_MS, String.valueOf(elapsedMs));
            log.info("http_request_completed");
            clearObservabilityMdc();
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        return firstValidHeader(request, CORRELATION_ID_HEADER)
            .or(() -> firstValidHeader(request, REQUEST_ID_HEADER))
            .orElseGet(() -> UUID.randomUUID().toString());
    }

    private Optional<String> firstValidHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        if (!SAFE_CORRELATION_ID.matcher(trimmed).matches()) {
            return Optional.empty();
        }

        return Optional.of(trimmed);
    }

    private void clearObservabilityMdc() {
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_REQUEST_METHOD);
        MDC.remove(MDC_REQUEST_URI);
        MDC.remove(MDC_STATUS_CODE);
        MDC.remove(MDC_ELAPSED_MS);
    }
}
