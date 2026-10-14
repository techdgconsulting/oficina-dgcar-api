package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.ClienteRepositoryPort;
import br.com.oficina.application.port.out.ExecucaoRepositoryPort;
import br.com.oficina.application.port.out.EntregaRepositoryPort;
import br.com.oficina.application.port.out.OrdemServicoMetricsPort;
import br.com.oficina.domain.model.Entrega;
import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.oficina.application.port.out.VeiculoRepositoryPort;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.valueobject.StatusOS;
import com.newrelic.api.agent.NewRelic;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdemServicoStatusSnapshotPublisher {

    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;
    private final ExecucaoRepositoryPort execucaoRepository;
    private final EntregaRepositoryPort entregaRepository;
    private final OrdemServicoMetricsPort metricsPort;

    @Scheduled(
            initialDelayString = "${observability.business-snapshot-initial-delay-ms:30000}",
            fixedDelayString = "${observability.business-snapshot-delay-ms:60000}")
    public void publicarSnapshotStatus() {
        for (StatusOS status : StatusOS.values()) {
            publicarStatus(status);
        }
        publicarOrdensOperacionais();
    }

    private void publicarStatus(StatusOS status) {
        try {
            int quantidade = ordemDeServicoRepository.listarPorStatus(status).size();
            NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsStatusSnapshot", Map.of(
                    "status", status.name(),
                    "quantidade", quantidade
            ));
        } catch (RuntimeException ex) {
            log.warn("NEW_RELIC_STATUS_SNAPSHOT_FALHOU status={} erro={}", status, ex.getMessage());
        }
    }

    private void publicarOrdensOperacionais() {
        try {
            ordemDeServicoRepository.listarTodas().forEach(os -> {
                try {
                    publicarOrdemOperacional(os);
                } catch (RuntimeException ex) {
                    log.warn("NEW_RELIC_OS_OPERATIONAL_SNAPSHOT_FALHOU numero_os={} erro={}",
                            os.getNumero(), ex.getMessage());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("NEW_RELIC_OS_OPERATIONAL_SNAPSHOT_FALHOU erro={}", ex.getMessage());
        }
    }

    private void publicarOrdemOperacional(OrdemDeServico os) {
        var cliente = clienteRepository.buscarPorId(os.getClienteId()).orElse(os.getCliente());
        var veiculo = veiculoRepository.buscarPorId(os.getVeiculoId()).orElse(os.getVeiculo());
        var mecanicoNome = execucaoRepository.buscarPorOrdemDeServico(os.getId())
                .map(execucao -> valor(execucao.getMecanicoNome()))
                .orElse("NAO_ATRIBUIDO");

        var dataEntrega = entregaRepository.buscarPorOrdemDeServico(os.getId())
                .map(Entrega::getDataEntrega).orElse(null);
        metricsPort.registrarSnapshotOperacional(os, cliente, veiculo, mecanicoNome, dataEntrega);
    }

    private String valor(String valor) {
        return valor != null && !valor.isBlank() ? valor : "NAO_INFORMADO";
    }
}
