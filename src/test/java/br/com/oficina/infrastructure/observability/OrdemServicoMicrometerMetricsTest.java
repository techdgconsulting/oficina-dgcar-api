package br.com.oficina.infrastructure.observability;

import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.model.Veiculo;
import br.com.oficina.domain.valueobject.CpfCnpj;
import br.com.oficina.domain.valueobject.Placa;
import br.com.oficina.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        metrics.registrarSnapshotOperacional(
                OrdemDeServico.builder()
                        .id(1L)
                        .numero("OS-2026-00001")
                        .status(StatusOS.EM_EXECUCAO)
                        .atendenteNome("Mariana Souza")
                        .dataCriacao(LocalDateTime.now().minusMinutes(20))
                        .build(),
                Cliente.builder()
                        .id(1L)
                        .documento(new CpfCnpj("52998224725"))
                        .nome("Ana Martins")
                        .build(),
                Veiculo.builder()
                        .id(1L)
                        .placa(new Placa("DGC0A26"))
                        .marca("Toyota")
                        .modelo("Corolla XEi")
                        .ano(2022)
                        .build(),
                "Carlos Silva", null);
        assertDoesNotThrow(() -> metrics.registrarSnapshotOperacional(
                OrdemDeServico.builder()
                        .numero(null)
                        .status(null)
                        .dataCriacao(null)
                        .build(),
                null,
                null,
                null,
                null));

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
