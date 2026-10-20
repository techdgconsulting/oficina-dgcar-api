package br.com.oficina.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void devePreservarCorrelationIdRecebidoNoHeader() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/ordens-servico/numero/OS-2026-00001");
        var response = new MockHttpServletResponse();
        var correlationIdCapturado = new AtomicReference<String>();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123");

        filter.doFilter(request, response, chainCapturandoMdc(correlationIdCapturado, 200));

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-123");
        assertThat(correlationIdCapturado.get()).isEqualTo("corr-123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void deveUsarRequestIdQuandoCorrelationIdNaoVierInformado() throws ServletException, IOException {
        var request = new MockHttpServletRequest("POST", "/api/ordens-servico/completa");
        var response = new MockHttpServletResponse();
        var correlationIdCapturado = new AtomicReference<String>();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "request-abc");

        filter.doFilter(request, response, chainCapturandoMdc(correlationIdCapturado, 201));

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("request-abc");
        assertThat(correlationIdCapturado.get()).isEqualTo("request-abc");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderNaoVierInformado() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();
        var correlationIdCapturado = new AtomicReference<String>();

        filter.doFilter(request, response, chainCapturandoMdc(correlationIdCapturado, 200));

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).matches(UUID_PATTERN);
        assertThat(correlationIdCapturado.get()).isEqualTo(correlationId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void deveIgnorarCorrelationIdInvalidoEGerarNovoValor() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/ordens-servico");
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "valor invalido com espaco");

        filter.doFilter(request, response, chainCapturandoMdc(new AtomicReference<>(), 200));

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).matches(UUID_PATTERN);
    }

    private FilterChain chainCapturandoMdc(AtomicReference<String> correlationIdCapturado, int statusCode) {
        return (request, response) -> {
            correlationIdCapturado.set(MDC.get("correlationId"));
            ((HttpServletResponse) response).setStatus(statusCode);
        };
    }
}
