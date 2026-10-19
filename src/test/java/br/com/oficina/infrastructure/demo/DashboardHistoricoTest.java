package br.com.oficina.infrastructure.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.adapters.in.web.controller.DashboardHistoricoController;
import br.com.oficina.application.port.out.EntregaRepositoryPort;
import br.com.oficina.application.port.out.ExecucaoRepositoryPort;
import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.oficina.domain.valueobject.StatusOS;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "oficina.dashboard.massa-historica.enabled=true",
        "observability.business-snapshot-initial-delay-ms=3600000"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DashboardHistoricoTest {

    private static final String URL = "/api/homologacao/dashboard/massa-historica";
    @Autowired private DashboardHistoricoService service;
    @Autowired private OrdemDeServicoRepositoryPort ordens;
    @Autowired private EntregaRepositoryPort entregas;
    @Autowired private ExecucaoRepositoryPort execucoes;
    @Autowired private MockMvc mvc;
    @Autowired private br.com.oficina.infrastructure.observability.OrdemServicoStatusSnapshotPublisher publisher;

    @Test
    void geraSeteDiasDistintosComCicloCompletoDeUmaSemanaSemDuplicar() {
        var primeira = service.gerar();
        var segunda = service.gerar();
        assertThat(primeira).hasSize(7);
        assertThat(primeira.stream().map(c -> c.dataAbertura().toLocalDate()).distinct()).hasSize(7);
        assertThat(segunda).extracting(DashboardHistoricoService.Cenario::id)
                .containsExactlyElementsOf(primeira.stream().map(DashboardHistoricoService.Cenario::id).toList());
        assertThat(ordens.listarTodas()).hasSize(7);
        for (var cenario : primeira) {
            assertThat(cenario.status()).isEqualTo(StatusOS.ENTREGUE);
            assertThat(cenario.diasAteEntrega()).isEqualTo(7);
            assertThat(Duration.between(cenario.dataAbertura(), cenario.dataEntrega())).isEqualTo(Duration.ofDays(7));
            var os = ordens.buscarPorId(cenario.id()).orElseThrow();
            assertThat(Duration.between(os.getDataCriacao(), entregas.buscarPorOrdemDeServico(os.getId())
                    .orElseThrow().getDataEntrega())).isEqualTo(Duration.ofDays(7));
            assertThat(execucoes.buscarPorOrdemDeServico(os.getId()).orElseThrow().getMecanicoNome()).isNotBlank();
            assertThat(os.calcularValorTotal()).isEqualByComparingTo("350.00");
        }
    }

    @Test
    void snapshotPublicaOsIntervalosPersistidosDaMassaHistorica() {
        var cenarios = service.gerar();
        var agent = org.mockito.Mockito.mock(com.newrelic.api.agent.Agent.class);
        var insights = org.mockito.Mockito.mock(com.newrelic.api.agent.Insights.class);
        try (var nr = org.mockito.Mockito.mockStatic(com.newrelic.api.agent.NewRelic.class)) {
            nr.when(com.newrelic.api.agent.NewRelic::getAgent).thenReturn(agent);
            org.mockito.Mockito.when(agent.getInsights()).thenReturn(insights);
            publisher.publicarSnapshotStatus();
            @SuppressWarnings("unchecked")
            org.mockito.ArgumentCaptor<java.util.Map<String, Object>> captor = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
            org.mockito.Mockito.verify(insights, org.mockito.Mockito.times(21)).recordCustomEvent(
                    org.mockito.ArgumentMatchers.eq("OficinaOsStatusDurationSnapshot"), captor.capture());
            var intervalos = captor.getAllValues();
            for (var cenario : cenarios) {
                var porOs = intervalos.stream().filter(i -> cenario.numero().equals(i.get("numero_os"))).toList();
                assertThat(porOs).hasSize(3);
                assertThat(porOs).anySatisfy(i -> {
                    assertThat(i.get("status")).isEqualTo("EM_DIAGNOSTICO");
                    assertThat(i.get("tempo_ms")).isEqualTo(Duration.ofHours(1).toMillis());
                }).anySatisfy(i -> {
                    assertThat(i.get("status")).isEqualTo("EM_EXECUCAO");
                    assertThat(i.get("tempo_ms")).isEqualTo(Duration.ofDays(6).minusHours(2).toMillis());
                }).anySatisfy(i -> {
                    assertThat(i.get("status")).isEqualTo("FINALIZADA");
                    assertThat(i.get("tempo_ms")).isEqualTo(Duration.ofHours(1).toMillis());
                });
                porOs.forEach(i -> assertThat(((Number)i.get("fim_ms")).longValue()
                        - ((Number)i.get("inicio_ms")).longValue()).isEqualTo(((Number)i.get("tempo_ms")).longValue()));
            }
        }
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void gestorPodeGerarMassa() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isOk());
        assertThat(ordens.listarTodas()).hasSize(7);
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoPodeGerarMassa() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isForbidden());
        assertThat(ordens.listarTodas()).isEmpty();
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void rotaEMassaFicamAusentesQuandoRecursoEstaDesabilitado() {
        new ApplicationContextRunner().withUserConfiguration(DashboardHistoricoService.class,
                        DashboardHistoricoController.class)
                .withPropertyValues("oficina.dashboard.massa-historica.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DashboardHistoricoController.class);
                    assertThat(context).doesNotHaveBean(DashboardHistoricoService.class);
                });
    }
}
