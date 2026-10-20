package br.com.oficina.infrastructure.observability;

import br.com.oficina.application.port.out.ClienteRepositoryPort;
import br.com.oficina.application.port.out.ExecucaoRepositoryPort;
import br.com.oficina.application.port.out.EntregaRepositoryPort;
import br.com.oficina.application.port.out.OrdemServicoMetricsPort;
import br.com.oficina.application.port.out.OrcamentoRepositoryPort;
import br.com.oficina.application.port.out.PagamentoRepositoryPort;
import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.oficina.application.port.out.VeiculoRepositoryPort;
import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.Execucao;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.model.Veiculo;
import br.com.oficina.domain.valueobject.CpfCnpj;
import br.com.oficina.domain.valueobject.Placa;
import br.com.oficina.domain.valueobject.StatusOS;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdemServicoStatusSnapshotPublisherTest {

    private final OrdemDeServicoRepositoryPort ordemRepository = mock(OrdemDeServicoRepositoryPort.class);
    private final ClienteRepositoryPort clienteRepository = mock(ClienteRepositoryPort.class);
    private final VeiculoRepositoryPort veiculoRepository = mock(VeiculoRepositoryPort.class);
    private final ExecucaoRepositoryPort execucaoRepository = mock(ExecucaoRepositoryPort.class);
    private final EntregaRepositoryPort entregaRepository = mock(EntregaRepositoryPort.class);
    private final OrdemServicoMetricsPort metricsPort = mock(OrdemServicoMetricsPort.class);
    private final OrcamentoRepositoryPort orcamentoRepository = mock(OrcamentoRepositoryPort.class);
    private final PagamentoRepositoryPort pagamentoRepository = mock(PagamentoRepositoryPort.class);
    private final OrdemServicoStatusSnapshotPublisher publisher = new OrdemServicoStatusSnapshotPublisher(
            ordemRepository,
            clienteRepository,
            veiculoRepository,
            execucaoRepository, entregaRepository, metricsPort, orcamentoRepository, pagamentoRepository);

    @Test
    void publicaSnapshotDeStatusEOrdensOperacionais() {
        var cliente = Cliente.builder()
                .id(10L)
                .documento(new CpfCnpj("52998224725"))
                .nome("Ana Martins")
                .email("ana.martins@email.com")
                .build();
        var veiculo = Veiculo.builder()
                .id(20L)
                .clienteId(10L)
                .placa(new Placa("DGC0A26"))
                .marca("Toyota")
                .modelo("Corolla XEi")
                .ano(2022)
                .build();
        var ordem = OrdemDeServico.builder()
                .id(30L)
                .numero("OS-2026-00001")
                .status(StatusOS.EM_EXECUCAO)
                .clienteId(10L)
                .cliente(cliente)
                .veiculoId(20L)
                .veiculo(veiculo)
                .atendenteNome("Mariana Souza")
                .dataCriacao(LocalDateTime.now().minusMinutes(45))
                .build();
        var execucao = Execucao.builder()
                .ordemDeServicoId(30L)
                .mecanicoNome("Carlos Silva")
                .build();

        when(ordemRepository.listarPorStatus(any(StatusOS.class))).thenReturn(List.of(ordem));
        when(ordemRepository.listarTodas()).thenReturn(List.of(ordem));
        when(clienteRepository.buscarPorId(10L)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.buscarPorId(20L)).thenReturn(Optional.of(veiculo));
        when(execucaoRepository.buscarPorOrdemDeServico(30L)).thenReturn(Optional.of(execucao));

        assertDoesNotThrow(publisher::publicarSnapshotStatus);

        verify(metricsPort).registrarSnapshotOperacional(ordem, cliente, veiculo, "Carlos Silva", null);
        verify(ordemRepository).listarTodas();
        verify(clienteRepository).buscarPorId(10L);
        verify(veiculoRepository).buscarPorId(20L);
        verify(execucaoRepository).buscarPorOrdemDeServico(30L);
    }

    @Test
    void ignoraFalhaAoPublicarSnapshotParaNaoQuebrarAgendamento() {
        when(ordemRepository.listarPorStatus(any(StatusOS.class))).thenThrow(new RuntimeException("fora"));
        when(ordemRepository.listarTodas()).thenThrow(new RuntimeException("fora"));

        assertDoesNotThrow(publisher::publicarSnapshotStatus);
    }
}
