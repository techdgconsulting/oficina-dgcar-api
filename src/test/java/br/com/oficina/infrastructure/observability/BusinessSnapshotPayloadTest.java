package br.com.oficina.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.valueobject.StatusOS;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BusinessSnapshotPayloadTest {

    @Test
    void publicaEntregaRealNoFusoDoNegocioEDatasDaCriacao() {
        var os = OrdemDeServico.builder().numero("OS-LOCAL-1").status(StatusOS.ENTREGUE)
                .dataCriacao(horarioServidor("2026-10-13T22:00:00"))
                .dataFinalizacao(LocalDateTime.now().minusDays(2)).build();
        var payload = publicar(os, horarioServidor("2026-10-14T22:30:00"));

        assertThat(payload).containsEntry("data_entrega", "14/10/2026 22:30:00")
                .containsEntry("data_criacao_dia", "2026-10-13")
                .containsEntry("snapshot_version", 2)
                .containsEntry("app_name", "oficina-dgcar-api-local")
                .containsEntry("finalizada_hoje", false);
        assertThat(payload).containsKey("dias_desde_criacao");
    }

    @Test
    void finalizadaHojeContinuaContadaAposEntregaESemEntregaNaoExibeData() {
        var os = OrdemDeServico.builder().numero("OS-LOCAL-2").status(StatusOS.FINALIZADA)
                .dataFinalizacao(LocalDateTime.now()).build();
        var payload = publicar(os, LocalDateTime.now());
        assertThat(payload).containsEntry("finalizada_hoje", true).containsEntry("data_entrega", "");
        os.aguardarRetirada();
        os.entregar();
        assertThat(publicar(os, LocalDateTime.now())).containsEntry("finalizada_hoje", true);
    }

    private Map<String, Object> publicar(OrdemDeServico os, LocalDateTime entrega) {
        var agent = mock(Agent.class);
        var insights = mock(Insights.class);
        try (var newRelic = mockStatic(NewRelic.class)) {
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getInsights()).thenReturn(insights);
            new OrdemServicoMicrometerMetrics(new SimpleMeterRegistry())
                    .registrarSnapshotOperacional(os, null, null, "Carlos Silva", entrega);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(insights).recordCustomEvent(eq("OficinaOsOperationalSnapshot"), captor.capture());
            return captor.getValue();
        }
    }

    private LocalDateTime horarioServidor(String horarioSaoPaulo) {
        return LocalDateTime.parse(horarioSaoPaulo).atZone(ZoneId.of("America/Sao_Paulo"))
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
