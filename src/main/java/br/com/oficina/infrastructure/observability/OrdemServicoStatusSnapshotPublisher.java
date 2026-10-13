package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.ClienteRepositoryPort;
import br.com.oficina.application.port.out.ExecucaoRepositoryPort;
import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.oficina.application.port.out.VeiculoRepositoryPort;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.valueobject.StatusOS;
import com.newrelic.api.agent.NewRelic;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
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
            ordemDeServicoRepository.listarTodas().forEach(this::publicarOrdemOperacional);
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

        Map<String, Object> atributos = new HashMap<>();
        atributos.put("numero_os", valor(os.getNumero()));
        atributos.put("status", os.getStatus().name());
        atributos.put("cliente_nome", cliente != null ? valor(cliente.getNome()) : "NAO_INFORMADO");
        atributos.put("veiculo", veiculo != null ? valor(veiculo.getMarca() + " " + veiculo.getModelo()) : "NAO_INFORMADO");
        atributos.put("placa", veiculo != null && veiculo.getPlaca() != null ? veiculo.getPlaca().getValor() : "NAO_INFORMADO");
        atributos.put("mecanico_nome", mecanicoNome);
        atributos.put("atendente_nome", valor(os.getAtendenteNome()));
        atributos.put("idade_minutos", idadeMinutos(os));
        atributos.put("valor_total", os.calcularValorTotal().doubleValue());
        NewRelic.getAgent().getInsights().recordCustomEvent("OficinaOsOperationalSnapshot", atributos);
    }

    private long idadeMinutos(OrdemDeServico os) {
        if (os.getDataCriacao() == null) {
            return 0;
        }
        var fim = os.getDataFinalizacao() != null ? os.getDataFinalizacao() : LocalDateTime.now();
        return Math.max(Duration.between(os.getDataCriacao(), fim).toMinutes(), 0);
    }

    private String valor(String valor) {
        return valor != null && !valor.isBlank() ? valor : "NAO_INFORMADO";
    }
}
