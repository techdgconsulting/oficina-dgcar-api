package br.com.oficina.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.oficina.adapters.out.persistence.jpa.ItemOSJpaEntity;
import br.com.oficina.adapters.out.persistence.jpa.OrdemDeServicoJpaEntity;
import br.com.oficina.adapters.out.persistence.mapper.OrdemDeServicoPersistenceMapper;
import br.com.oficina.adapters.out.persistence.repository.SpringDataOrdemDeServicoRepository;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.valueobject.StatusOS;
import br.com.oficina.domain.valueobject.TipoItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({OrdemDeServicoPersistenceAdapter.class, OrdemDeServicoPersistenceMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrdemDeServicoPersistenceTransactionTest {

    @Autowired
    private OrdemDeServicoPersistenceAdapter adapter;

    @Autowired
    private SpringDataOrdemDeServicoRepository repository;

    @Test
    void deveMapearItensSemTransacaoNoChamadorComoNoAgendador() {
        var entity = new OrdemDeServicoJpaEntity();
        entity.setNumero("OS-SNAPSHOT-REGRESSION");
        entity.setStatus(StatusOS.EM_EXECUCAO);
        entity.setDataCriacao(LocalDateTime.now());
        var entradaStatus = LocalDateTime.of(2026, 10, 14, 10, 30);
        entity.setDataEntradaStatus(entradaStatus);
        entity.setClienteId(1L);
        entity.setVeiculoId(2L);
        entity.adicionarItem(new ItemOSJpaEntity(null, TipoItem.SERVICO, "Revisao", 2,
                new BigDecimal("75.00"), null, false, null));
        var saved = repository.saveAndFlush(entity);

        try {
            verificarItens(adapter.listarTodas().get(0));
            assertThat(adapter.buscarPorId(saved.getId()).orElseThrow().getDataEntradaStatus())
                    .isEqualTo(entradaStatus);
            verificarItens(adapter.listarPorStatus(StatusOS.EM_EXECUCAO).get(0));
            verificarItens(adapter.buscarPorId(saved.getId()).orElseThrow());
            verificarItens(adapter.buscarPorNumero(saved.getNumero()).orElseThrow());
            verificarItens(adapter.listarPorCliente(1L).get(0));
            verificarItens(adapter.listarPorVeiculo(2L).get(0));
            verificarItens(adapter.listarFilaOperacional().get(0));
            verificarItens(adapter.salvar(adapter.buscarPorId(saved.getId()).orElseThrow()));
        } finally {
            repository.deleteById(saved.getId());
        }
    }

    private void verificarItens(OrdemDeServico os) {
        assertThat(os.getItens()).hasSize(1);
        assertThat(os.getItens().get(0).getDescricao()).isEqualTo("Revisao");
        assertThat(os.calcularValorTotal()).isEqualByComparingTo("150.00");
    }
}
