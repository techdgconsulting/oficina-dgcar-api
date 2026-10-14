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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
