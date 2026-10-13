package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.OrdemServicoMetricsPort;
import br.com.oficina.domain.valueobject.StatusOS;
import com.newrelic.api.agent.NewRelic;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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
        registrarEventoNewRelic("OS_CRIADA", Map.of(
                "origem", valor(origem),
                "status", status(status)));
    }

    @Override
    public void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior,
            String mecanicoNome) {
        Counter.builder("oficina_os_status_alterado_total")
                .description("Total de mudancas de status de ordens de servico")
                .tag("fluxo", valor(fluxo))
                .tag("status_anterior", status(statusAnterior))
                .tag("status_atual", status(statusAtual))
                .tag("mecanico_nome", valor(mecanicoNome))
                .register(meterRegistry)
                .increment();
        registrarEventoNewRelic("STATUS_ALTERADO", Map.of(
                "fluxo", valor(fluxo),
                "status_anterior", status(statusAnterior),
                "status_atual", status(statusAtual),
                "mecanico_nome", valor(mecanicoNome)));

        if (tempoNoStatusAnterior != null && !tempoNoStatusAnterior.isNegative()) {
            Timer.builder("oficina_os_tempo_status")
                    .description("Tempo observado ate a saida do status anterior da ordem de servico")
                    .tag("fluxo", valor(fluxo))
                    .tag("status", status(statusAnterior))
                    .tag("mecanico_nome", valor(mecanicoNome))
                    .register(meterRegistry)
                    .record(tempoNoStatusAnterior);
            registrarEventoNewRelic("TEMPO_STATUS", Map.of(
                    "fluxo", valor(fluxo),
                    "status", status(statusAnterior),
                    "tempo_ms", tempoNoStatusAnterior.toMillis(),
                    "mecanico_nome", valor(mecanicoNome)));
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
        registrarEventoNewRelic("FALHA_PROCESSAMENTO", Map.of(
                "fluxo", valor(fluxo),
                "erro", erro(erro)));
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
        registrarEventoNewRelic("FALHA_NOTIFICACAO", Map.of(
                "tipo", valor(tipo),
                "status", status(status),
                "erro", erro(erro)));
    }

    @Override
    public void registrarFalhaPagamento(String motivo, Throwable erro) {
        Counter.builder("oficina_os_pagamento_falha_total")
                .description("Total de falhas de pagamento ou integracao com gateway de pagamento")
                .tag("motivo", valor(motivo))
                .tag("erro", erro(erro))
                .register(meterRegistry)
                .increment();
        registrarEventoNewRelic("FALHA_PAGAMENTO", Map.of(
                "motivo", valor(motivo),
                "erro", erro(erro)));
    }

    private void registrarEventoNewRelic(String evento, Map<String, Object> atributos) {
        Map<String, Object> eventoAtributos = new HashMap<>(atributos);
        eventoAtributos.put("evento", evento);
        NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsEvent", eventoAtributos);
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
