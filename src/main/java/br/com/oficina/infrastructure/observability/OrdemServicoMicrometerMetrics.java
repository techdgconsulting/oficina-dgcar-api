package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.OrdemServicoMetricsPort;
import br.com.oficina.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrdemServicoMicrometerMetrics implements OrdemServicoMetricsPort {

    private final MeterRegistry meterRegistry;

    @Override
    public void registrarOsCriada(String origem, StatusOS status) {
        Counter.builder("oficina_os_criada_total")
                .description("Total de ordens de servico criadas")
                .tag("origem", valor(origem))
                .tag("status", status(status))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior) {
        Counter.builder("oficina_os_status_alterado_total")
                .description("Total de mudancas de status de ordens de servico")
                .tag("fluxo", valor(fluxo))
                .tag("status_anterior", status(statusAnterior))
                .tag("status_atual", status(statusAtual))
                .register(meterRegistry)
                .increment();

        if (tempoNoStatusAnterior != null && !tempoNoStatusAnterior.isNegative()) {
            Timer.builder("oficina_os_tempo_status")
                    .description("Tempo observado ate a saida do status anterior da ordem de servico")
                    .tag("fluxo", valor(fluxo))
                    .tag("status", status(statusAnterior))
                    .register(meterRegistry)
                    .record(tempoNoStatusAnterior);
        }
    }

    @Override
    public void registrarFalhaProcessamento(String fluxo, Throwable erro) {
        Counter.builder("oficina_os_processamento_falha_total")
                .description("Total de falhas no processamento de ordens de servico")
                .tag("fluxo", valor(fluxo))
                .tag("erro", erro(erro))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarFalhaNotificacao(String tipo, StatusOS status, Throwable erro) {
        Counter.builder("oficina_os_notificacao_falha_total")
                .description("Total de falhas no envio de notificacoes de ordens de servico")
                .tag("tipo", valor(tipo))
                .tag("status", status(status))
                .tag("erro", erro(erro))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarFalhaPagamento(String motivo, Throwable erro) {
        Counter.builder("oficina_os_pagamento_falha_total")
                .description("Total de falhas de pagamento ou integracao com gateway de pagamento")
                .tag("motivo", valor(motivo))
                .tag("erro", erro(erro))
                .register(meterRegistry)
                .increment();
    }

    private static String status(StatusOS status) {
        return status != null ? status.name() : "NAO_INFORMADO";
    }

    private static String erro(Throwable erro) {
        return erro != null ? erro.getClass().getSimpleName() : "NAO_INFORMADO";
    }

    private static String valor(String valor) {
        return valor != null && !valor.isBlank() ? valor : "NAO_INFORMADO";
    }
}
