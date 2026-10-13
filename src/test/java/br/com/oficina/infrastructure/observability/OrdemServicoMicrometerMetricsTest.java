package br.com.oficina.infrastructure.observability;

import br.com.oficina.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdemServicoMicrometerMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OrdemServicoMicrometerMetrics metrics = new OrdemServicoMicrometerMetrics(meterRegistry);

    @Test
    void registraEventosDeNegocioDaOrdemDeServico() {
        metrics.registrarOsCriada("completa", StatusOS.RECEBIDA, "OS-2026-00001");
        metrics.registrarMudancaStatus(
                "iniciar_diagnostico",
                StatusOS.RECEBIDA,
                StatusOS.EM_DIAGNOSTICO,
                Duration.ofSeconds(30),
                "Joao Mecanico",
                "OS-2026-00001");
        metrics.registrarFalhaProcessamento("gerar_orcamento", new IllegalStateException("status invalido"));
        metrics.registrarFalhaNotificacao("status_os", StatusOS.EM_EXECUCAO, new RuntimeException("smtp"));
        metrics.registrarFalhaPagamento("recusado_gateway", null);

        assertEquals(1.0, meterRegistry.counter(
                "oficina_os_criada_total",
                "origem", "completa",
                "status", "RECEBIDA").count());
        assertEquals(1.0, meterRegistry.counter(
                "oficina_os_status_alterado_total",
                "fluxo", "iniciar_diagnostico",
                "status_anterior", "RECEBIDA",
                "status_atual", "EM_DIAGNOSTICO",
                "mecanico_nome", "Joao Mecanico").count());
        assertEquals(1, meterRegistry.timer(
                "oficina_os_tempo_status",
                "fluxo", "iniciar_diagnostico",
                "status", "RECEBIDA",
                "mecanico_nome", "Joao Mecanico").count());
        assertEquals(1.0, meterRegistry.counter(
                "oficina_os_processamento_falha_total",
                "fluxo", "gerar_orcamento",
                "erro", "IllegalStateException").count());
        assertEquals(1.0, meterRegistry.counter(
                "oficina_os_notificacao_falha_total",
                "tipo", "status_os",
                "status", "EM_EXECUCAO",
                "erro", "RuntimeException").count());
        assertEquals(1.0, meterRegistry.counter(
                "oficina_os_pagamento_falha_total",
                "motivo", "recusado_gateway",
                "erro", "NAO_INFORMADO").count());
    }
}
