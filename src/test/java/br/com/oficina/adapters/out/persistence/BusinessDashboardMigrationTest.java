package br.com.oficina.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class BusinessDashboardMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migraBancoExistentePreservandoDatasECompletandoSomenteMassaOriginal() throws Exception {
        var flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        flyway.target("19").load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            // Simula uma atribuicao real posterior a V6: a migracao nao pode sobrescreve-la.
            statement.executeUpdate("UPDATE execucoes SET mecanico_nome = 'Responsavel preservado' WHERE ordem_servico_id = (SELECT MIN(id) FROM ordens_servico)");
            Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM execucoes WHERE mecanico_nome IS NULL OR TRIM(mecanico_nome) = ''")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
            }
            try (var rows = statement.executeQuery("SELECT mecanico_nome FROM execucoes WHERE ordem_servico_id = (SELECT MIN(id) FROM ordens_servico)")) {
                rows.next();
                assertThat(rows.getString(1)).isEqualTo("Responsavel preservado");
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM ordens_servico WHERE data_entrada_status IS NOT NULL")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }
}
