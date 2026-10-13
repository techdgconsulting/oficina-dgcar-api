package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
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

    @Scheduled(
            initialDelayString = "${observability.business-snapshot-initial-delay-ms:30000}",
            fixedDelayString = "${observability.business-snapshot-delay-ms:60000}")
    public void publicarSnapshotStatus() {
        for (StatusOS status : StatusOS.values()) {
            publicarStatus(status);
        }
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
}
