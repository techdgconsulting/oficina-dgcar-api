package br.com.oficina.infrastructure.demo;

import br.com.oficina.application.port.out.ClienteRepositoryPort;
import br.com.oficina.application.port.out.EncerramentoRepositoryPort;
import br.com.oficina.application.port.out.EntregaRepositoryPort;
import br.com.oficina.application.port.out.ExecucaoRepositoryPort;
import br.com.oficina.application.port.out.OrcamentoRepositoryPort;
import br.com.oficina.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.oficina.application.port.out.PagamentoRepositoryPort;
import br.com.oficina.application.port.out.VeiculoRepositoryPort;
import br.com.oficina.domain.model.Cliente;
import br.com.oficina.domain.model.Diagnostico;
import br.com.oficina.domain.model.Encerramento;
import br.com.oficina.domain.model.Entrega;
import br.com.oficina.domain.model.Execucao;
import br.com.oficina.domain.model.ItemOS;
import br.com.oficina.domain.model.Orcamento;
import br.com.oficina.domain.model.OrdemDeServico;
import br.com.oficina.domain.model.Pagamento;
import br.com.oficina.domain.model.Veiculo;
import br.com.oficina.domain.valueobject.CpfCnpj;
import br.com.oficina.domain.valueobject.MetodoPagamento;
import br.com.oficina.domain.valueobject.PeriodoExecucao;
import br.com.oficina.domain.valueobject.Placa;
import br.com.oficina.domain.valueobject.StatusDiagnostico;
import br.com.oficina.domain.valueobject.StatusEncerramento;
import br.com.oficina.domain.valueobject.StatusEntrega;
import br.com.oficina.domain.valueobject.StatusExecucao;
import br.com.oficina.domain.valueobject.StatusOS;
import br.com.oficina.domain.valueobject.StatusOrcamento;
import br.com.oficina.domain.valueobject.StatusPagamento;
import br.com.oficina.domain.valueobject.TipoItem;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oficina.dashboard.massa-historica.enabled", havingValue = "true")
public class DashboardHistoricoService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String DOCUMENTO = "12345678909";
    private static final String CLIENTE = "[DEMO] Historico Dashboard";
    private static final BigDecimal VALOR = new BigDecimal("350.00");
    private final ClienteRepositoryPort clientes;
    private final VeiculoRepositoryPort veiculos;
    private final OrdemDeServicoRepositoryPort ordens;
    private final OrcamentoRepositoryPort orcamentos;
    private final ExecucaoRepositoryPort execucoes;
    private final PagamentoRepositoryPort pagamentos;
    private final EntregaRepositoryPort entregas;
    private final EncerramentoRepositoryPort encerramentos;

    @Transactional
    public List<Cenario> gerar() {
        var cliente = clientes.buscarPorDocumento(DOCUMENTO).orElseGet(() ->
                clientes.salvar(new Cliente(new CpfCnpj(DOCUMENTO), CLIENTE, "11999990000", "historico@example.invalid")));
        if (!CLIENTE.equals(cliente.getNome())) {
            throw new IllegalStateException("Documento da massa historica ja pertence a outro cliente");
        }
        var referencia = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        var resultado = new ArrayList<Cenario>();
        for (int dia = 0; dia < 7; dia++) {
            var entrega = servidor(referencia.minusDays(dia));
            var abertura = servidor(referencia.minusDays(dia + 7));
            var numero = "OS-HIST-" + referencia.minusDays(dia + 7).toLocalDate();
            var existente = ordens.buscarPorNumero(numero);
            OrdemDeServico os;
            if (existente.isPresent()) {
                os = existente.orElseThrow();
                if (!cliente.getId().equals(os.getClienteId())) {
                    throw new IllegalStateException("Numero da massa historica ja pertence a outra OS");
                }
                entrega = entregas.buscarPorOrdemDeServico(os.getId()).orElseThrow().getDataEntrega();
            } else {
                os = criar(cliente, dia, numero, abertura, entrega);
            }
            resultado.add(new Cenario(os.getId(), os.getNumero(), os.getStatus(), negocio(os.getDataCriacao()),
                    negocio(entrega), Duration.between(os.getDataCriacao(), entrega).toDays()));
        }
        return resultado;
    }

    private OrdemDeServico criar(Cliente cliente, int dia, String numero, LocalDateTime abertura, LocalDateTime entrega) {
        var placa = "HIS" + dia + "A26";
        var veiculo = veiculos.buscarPorPlaca(placa).orElseGet(() -> veiculos.salvar(Veiculo.builder()
                .clienteId(cliente.getId()).placa(new Placa(placa)).marca("Demo").modelo("Historico " + dia).ano(2026).build()));
        if (!cliente.getId().equals(veiculo.getClienteId())) {
            throw new IllegalStateException("Placa da massa historica ja pertence a outro cliente");
        }
        var fimServico = entrega.minusHours(2);
        var os = OrdemDeServico.builder().numero(numero).clienteId(cliente.getId()).veiculoId(veiculo.getId())
                .status(StatusOS.ENTREGUE).dataCriacao(abertura).dataFinalizacao(fimServico)
                .dataEntradaStatus(entrega).atendenteNome("Mariana Souza").build();
        os.adicionarItem(ItemOS.builder().tipo(TipoItem.SERVICO).descricao("[DEMO] Revisao historica")
                .quantidade(1).valorUnitario(VALOR).build());
        os = ordens.salvar(os);
        orcamentos.salvar(Orcamento.builder().ordemDeServicoId(os.getId()).status(StatusOrcamento.APROVADO)
                .valorTotal(VALOR).dataCriacao(abertura.plusHours(2)).dataAprovacao(abertura.plusDays(1))
                .dataValidade(abertura.plusDays(7)).build());
        execucoes.salvar(Execucao.builder().ordemDeServicoId(os.getId()).status(StatusExecucao.SERVICO_FINALIZADO)
                .mecanicoNome(List.of("Carlos Silva", "Rafael Costa", "Marcos Santos").get(dia % 3))
                .diagnostico(Diagnostico.builder().status(StatusDiagnostico.CONCLUIDO)
                        .descricaoProblema("[DEMO] Revisao historica").dataDiagnostico(abertura.plusHours(1)).build())
                .periodoExecucao(new PeriodoExecucao(abertura.plusDays(1), fimServico)).build());
        pagamentos.salvar(Pagamento.builder().ordemDeServicoId(os.getId()).status(StatusPagamento.APROVADO)
                .metodo(MetodoPagamento.PIX).valor(VALOR).dataPagamento(entrega.minusHours(1))
                .transactionId("DEMO-" + numero).gatewayMensagem("Massa sintetica de homologacao").build());
        entregas.salvar(Entrega.builder().ordemDeServicoId(os.getId()).status(StatusEntrega.VEICULO_ENTREGUE)
                .dataEntrega(entrega).observacoes("[DEMO] Entrega sete dias apos abertura").build());
        encerramentos.salvar(Encerramento.builder().ordemDeServicoId(os.getId()).status(StatusEncerramento.ENCERRADA)
                .dataEncerramento(entrega).build());
        return os;
    }

    private LocalDateTime servidor(LocalDateTime data) {
        return data.atZone(BUSINESS_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private OffsetDateTime negocio(LocalDateTime data) {
        return data.atZone(ZoneId.systemDefault()).withZoneSameInstant(BUSINESS_ZONE).toOffsetDateTime();
    }

    public record Cenario(Long id, String numero, StatusOS status, OffsetDateTime dataAbertura,
                          OffsetDateTime dataEntrega, long diasAteEntrega) { }
}
