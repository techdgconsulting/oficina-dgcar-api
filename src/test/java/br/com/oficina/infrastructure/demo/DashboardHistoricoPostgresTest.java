package br.com.oficina.infrastructure.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "oficina.dashboard.massa-historica.enabled=true",
        "observability.business-snapshot-initial-delay-ms=3600000"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class DashboardHistoricoPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired private DashboardHistoricoService service;
    @Autowired private OrdemDeServicoRepositoryPort ordens;

    @DynamicPropertySource
    static void bancoReal(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void persisteMassaHistoricaNoSchemaRealSemDuplicarOuAlterarMassaExistente() {
        int anteriores = ordens.listarTodas().size();
        var primeira = service.gerar();
        assertThat(service.gerar()).extracting(DashboardHistoricoService.Cenario::id)
                .containsExactlyElementsOf(primeira.stream().map(DashboardHistoricoService.Cenario::id).toList());
        assertThat(primeira).hasSize(7).allSatisfy(cenario -> {
            assertThat(cenario.numero().length()).isLessThanOrEqualTo(20);
            assertThat(cenario.diasAteEntrega()).isEqualTo(7);
        });
        assertThat(ordens.listarTodas()).hasSize(anteriores + 7);
    }
}
