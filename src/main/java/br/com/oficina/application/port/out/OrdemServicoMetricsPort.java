package br.com.oficina.application.port.out;

import br.com.oficina.domain.valueobject.StatusOS;
import java.time.Duration;

public interface OrdemServicoMetricsPort {

    OrdemServicoMetricsPort NOOP = new OrdemServicoMetricsPort() {
    };

    default void registrarOsCriada(String origem, StatusOS status) {
    }

    default void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior) {
        registrarMudancaStatus(fluxo, statusAnterior, statusAtual, tempoNoStatusAnterior, null);
    }

    default void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior,
            String mecanicoNome) {
    }

    default void registrarFalhaProcessamento(String fluxo, Throwable erro) {
    }

    default void registrarFalhaNotificacao(String tipo, StatusOS status, Throwable erro) {
    }

    default void registrarFalhaPagamento(String motivo, Throwable erro) {
    }
}
