# Validacao do dashboard de negocio

## Definicoes

- Aberta: ultimo status diferente de ENTREGUE e CANCELADA; inclui servicos finalizados ainda na oficina.
- Cabecalho: somente o card Total de OS em aberto, ao lado de Status das OS. Os cards Em execucao, Aguardando aprovacao e Concluidas hoje foram removidos.
- Volume diario: OS distintas pela data real de criacao, nos ultimos 14 dias de calendario. Usa snapshots do banco, incluindo OS criadas antes da instrumentacao. Os 30 minutos da query sao a janela de recebimento dos snapshots, nao a data de criacao.
- Tempo por status: intervalo entre entrada e saida do status. Finalizacao representa a permanencia em FINALIZADA ate o pagamento/aguardo de retirada. Apenas eventos metrics_version=2 entram na media; duracoes antigas incorretas ficam excluidas.
- OS antigas sem horario confiavel de entrada no status passam a registrar tempos apos a primeira transicao. Nao ha estimativa retroativa.
- Data de entrega: campo real da entidade Entrega, convertido do fuso do servidor para America/Sao_Paulo. Fica vazia nas OS nao entregues.
- A tabela inclui OS ativas e entregues; canceladas ficam fora. A consulta considera o ultimo estado por OS.

## Validacao

1. Executar `mvn clean test`. O teste PostgreSQL de migracao precisa de Docker; tambem roda no GitHub Actions.
2. Executar no Postman a pasta **Popular Dashboard Negocio OS**, com baseUrl do ambiente e intervalo de 1000 ms entre requisicoes.
3. Conferir as assercoes de mecanico: cada diagnostico iniciado deve retornar o nome do usuario mecanico autenticado.
4. Aguardar dois ciclos de snapshot. Conferir ausencia de SNAPSHOT_FALHOU e LazyInitializationException nos logs.
5. Comparar o ultimo status de cada OS com os cards e o grafico de status. Uma OS entregue nao deve continuar sendo contada em execucao.
6. Conferir a data real de entrega na tabela e a ausencia da coluna Tempo min.
7. Conferir o volume do dia com a quantidade de OS criadas. Uma execucao feita hoje gera uma barra de hoje; varios dias exigem criacoes reais em varios dias.
8. Conferir tempos de diagnostico, execucao e finalizacao apos suas respectivas transicoes. Sem saida do status ainda nao ha duracao concluida para aquela etapa.
9. Conferir falhas reais de notificacao e pagamento e os eventos FALHA_PROCESSAMENTO, FALHA_NOTIFICACAO e FALHA_PAGAMENTO. Zero falhas nao deve ser confundido com ausencia de telemetria; validar tambem snapshots e eventos de sucesso no mesmo ambiente.

A massa original da V6 recebe mecanicos somente quando placa e data de criacao coincidem e o responsavel esta vazio. Atribuicoes existentes sao preservadas.

## Massa historica pela collection

Executar a pasta **Popular Dashboard Historico - Ciclo de 7 dias**. A pasta faz login da gestora, gera sete OS, repete a chamada para validar idempotencia e confere as OS na listagem. As aberturas sao D-7 a D-13; as entregas sao hoje a D-6. Cada entrega ocorre exatamente sete dias apos a abertura.

A rota `POST /api/homologacao/dashboard/massa-historica` aceita somente GESTOR e depende de `OFICINA_DASHBOARD_MASSA_HISTORICA_ENABLED=true`. O deploy habilita a opcao somente em homolog; producao e execucoes locais ficam desabilitadas por padrao. No Docker Compose local a opcao pode ser configurada explicitamente.

A massa e identificada por prefixo OS-HIST, cliente demonstrativo e placas HIS. Reexecutar no mesmo dia preserva IDs e datas. Um conflito com cliente, veiculo ou OS de outra origem interrompe a transacao. Pagamentos e notificacoes externas nao sao acionados. O agendador publica os snapshots apos o commit da massa.
