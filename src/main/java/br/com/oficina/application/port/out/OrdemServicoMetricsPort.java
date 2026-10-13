package br.com.oficina.application.port.out;

import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.model.Veiculo;
import br.com.oficina.domain.valueobject.StatusOS;
import java.time.Duration;

public interface OrdemServicoMetricsPort {

    OrdemServicoMetricsPort NOOP = new OrdemServicoMetricsPort() {
    };

    default void registrarOsCriada(String origem, StatusOS status) {
        registrarOsCriada(origem, status, null);
    }

    default void registrarOsCriada(String origem, StatusOS status, String numeroOs) {
    }

    default void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior) {
        registrarMudancaStatus(fluxo, statusAnterior, statusAtual, tempoNoStatusAnterior, null, null);
    }

    default void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior,
            String mecanicoNome) {
        registrarMudancaStatus(fluxo, statusAnterior, statusAtual, tempoNoStatusAnterior, mecanicoNome, null);
    }

    default void registrarMudancaStatus(
            String fluxo,
            StatusOS statusAnterior,
            StatusOS statusAtual,
            Duration tempoNoStatusAnterior,
            String mecanicoNome,
            String numeroOs) {
    }

    default void registrarSnapshotOperacional(
            OrdemDeServico os,
            Cliente cliente,
            Veiculo veiculo,
            String mecanicoNome) {
    }

    default void registrarFalhaProcessamento(String fluxo, Throwable erro) {
    }

    default void registrarFalhaNotificacao(String tipo, StatusOS status, Throwable erro) {
    }

    default void registrarFalhaPagamento(String motivo, Throwable erro) {
    }
}
