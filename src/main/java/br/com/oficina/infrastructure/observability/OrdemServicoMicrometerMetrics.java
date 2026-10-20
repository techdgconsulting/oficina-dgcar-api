package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.OrdemServicoMetricsPort;
import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.model.Veiculo;
import br.com.oficina.domain.valueobject.StatusOS;
import com.newrelic.api.agent.NewRelic;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class OrdemServicoMicrometerMetrics implements OrdemServicoMetricsPort {

    private final MeterRegistry meterRegistry;

    @Value("${NEW_RELIC_APP_NAME:oficina-dgcar-api-local}")
    private String appName = "oficina-dgcar-api-local";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Override
    public void registrarOsCriada(String origem, StatusOS status, String numeroOs) {
        Counter.builder("oficina_os_criada_total")
                .description("Total de ordens de servico criadas")
                .tag("origem", valor(origem))
                .tag("status", status(status))
                .register(meterRegistry)
                .increment();
        registrarEventoNewRelic("OS_CRIADA", Map.of(
                "origem", valor(origem),
                "status", status(status),
                "numero_os", valor(numeroOs)));
    }

    @Override
    public void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior,
            String mecanicoNome,
            String numeroOs) {
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
                "mecanico_nome", valor(mecanicoNome),
                "numero_os", valor(numeroOs)));

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
                    "mecanico_nome", valor(mecanicoNome),
                    "numero_os", valor(numeroOs)));
        }
    }

    @Override
    public void registrarSnapshotOperacional(
            OrdemDeServico os,
            Cliente cliente,
            Veiculo veiculo,
            String mecanicoNome,
            LocalDateTime dataEntrega) {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("numero_os", valor(os.getNumero()));
        atributos.put("status", status(os.getStatus()));
        atributos.put("cliente_nome", cliente != null ? valor(cliente.getNome()) : "NAO_INFORMADO");
        atributos.put("veiculo", veiculo != null ? valor(veiculo.getMarca() + " " + veiculo.getModelo()) : "NAO_INFORMADO");
        atributos.put("placa", veiculo != null && veiculo.getPlaca() != null ? veiculo.getPlaca().getValor() : "NAO_INFORMADO");
        atributos.put("mecanico_nome", valor(mecanicoNome));
        atributos.put("atendente_nome", valor(os.getAtendenteNome()));
        atributos.put("idade_minutos", idadeMinutos(os));
        atributos.put("valor_total", os.calcularValorTotal().doubleValue());
        atributos.put("app_name", appName);
        atributos.put("snapshot_version", 2);
        if (os.getDataCriacao() != null) {
            var dataCriacao = dataNegocio(os.getDataCriacao());
            atributos.put("data_criacao_dia", dataCriacao.toLocalDate().toString());
            atributos.put("dias_desde_criacao", ChronoUnit.DAYS.between(
                    dataCriacao.toLocalDate(), LocalDate.now(BUSINESS_ZONE)));
        }
        atributos.put("finalizada_hoje", os.getDataFinalizacao() != null
                && dataNegocio(os.getDataFinalizacao()).toLocalDate().equals(LocalDate.now(BUSINESS_ZONE)));
        atributos.put("data_finalizacao_ms", os.getDataFinalizacao() != null
                ? epochMillis(os.getDataFinalizacao()) : -1L);
        // O valor vazio tambem precisa ser publicado, para latest() nao reutilizar dados antigos.
        atributos.put("data_entrega", os.getStatus() == StatusOS.ENTREGUE && dataEntrega != null
                ? dataNegocio(dataEntrega).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsOperationalSnapshot", atributos);
    }

    @Override
    public void registrarTempoStatusPersistido(String numeroOs, StatusOS status,
                                              LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            return;
        }
        NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsStatusDurationSnapshot", Map.of(
                "app_name", appName,
                "numero_os", valor(numeroOs),
                "status", status(status),
                "inicio_ms", epochMillis(inicio),
                "fim_ms", epochMillis(fim),
                "tempo_ms", Duration.between(inicio, fim).toMillis()));
    }

    private static long epochMillis(LocalDateTime data) {
        return data.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
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
        eventoAtributos.put("app_name", appName);
        eventoAtributos.put("metrics_version", 2);
        NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsEvent", eventoAtributos);
    }

    private static LocalDateTime dataNegocio(LocalDateTime data) {
        return data.atZone(ZoneId.systemDefault()).withZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
    }

    private static long idadeMinutos(OrdemDeServico os) {
        if (os.getDataCriacao() == null) {
            return 0;
        }
        var fim = os.getDataFinalizacao() != null ? os.getDataFinalizacao() : LocalDateTime.now();
        return Math.max(Duration.between(os.getDataCriacao(), fim).toMinutes(), 0);
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
