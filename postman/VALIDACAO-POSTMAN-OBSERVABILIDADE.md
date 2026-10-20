# HPA e falhas de processamento no Postman

Importe novamente `Oficina-DGCar-Tech-Challenge-Fase-3.postman_collection.json`.
Configure `baseUrl` com a URL do ambiente e as variaveis
`observabilidadeUsuario` e `observabilidadeSenha` com um usuario GESTOR.
O valor inicial usa Patricia Almeida, presente na massa de homologacao.

## Carga para o HPA

1. Execute `HPA - Teste de carga / 00 - Preparacao (executar uma vez)` no modo
   Functional. O login guarda `hpaTokenGestor`; a consulta seguinte verifica
   acesso e existencia de OS. Se nao houver OS, execute a massa de negocio antes.
2. Selecione somente a subpasta `01 - Carga (somente esta pasta no Performance Runner)`.
   Use **Run > Performance**, metodo **App**.
3. Configure **Ramp up**, carga inicial **5**, maximo **40 usuarios**, duracao
   **9 minutos**. Ajuste a rampa para aproximadamente **3min30s** e mantenha a
   carga maxima no restante. Nas configuracoes do Postman, use timeout de
   requisicao de **15000 ms**. Confira os testes de resposta 200 e mantenha
   erros HTTP abaixo de 1%.
4. Acompanhe CPU e pods ativos no dashboard. Para comprovar readiness, uso da
   mesma maquina e ausencia de reinicios, acompanhe tambem Kubernetes:

   ```powershell
   kubectl -n oficina get hpa oficina-api -w
   kubectl -n oficina get pods -l app.kubernetes.io/name=oficina-api -o wide -w
   kubectl -n oficina top pods
   ```

5. Encerre a carga e observe a reducao automatica. A janela de estabilizacao
   de scale-down e de aproximadamente cinco minutos, alem do tempo de coleta.

Somente o GET de OS fica no loop. Cada chamada recebe um `X-Correlation-Id`
novo. A pasta nao altera OS, HPA, Deployment ou infraestrutura. O HPA mantem
seu maximo configurado (atualmente 3), portanto esta carga nao garante que a
escala pare exatamente em 2. O alvo de CPU e 70% do request de 250m, equivalente
a 175m por pod; o limite de 500m nao e a base desse percentual.

O perfil acima e uma aproximacao no Postman. O ensaio k6 validado usa estes
estagios, com pausa de 50ms entre iteracoes:

| Duracao do estagio | Usuarios ao final |
| --- | ---: |
| 30 segundos | 5 |
| 1 minuto | 10 |
| 2 minutos | 20 |
| 5 minutos | 40 |
| 30 segundos | 0 |

O JSON da collection inclui as chamadas, scripts e instrucoes. Perfil, usuarios,
duracao e timeout precisam ser configurados no Runner; nao sao ativados por
variaveis da collection. O Runner Functional sequencial nao equivale a usuarios
simultaneos. Nao selecione a collection inteira para o teste de desempenho.

Para repetir os estagios exatos e a verificacao automatica de
**1 -> 2 Ready -> 1 na mesma EC2**, execute o workflow
`Validar Escala HPA Homolog` com `action=test-only` no repositorio de infraestrutura.
Ele limita temporariamente o maximo a 2, verifica a escala e restaura o HPA.

Referencia: [Configuracao do Performance Runner](https://learning.postman.com/docs/tests-and-scripts/performance-testing/performance-test-configuration/).

## OS com falhas de processamento

Execute a pasta `OS - Falhas de processamento` inteira, no modo **Functional**,
uma iteracao. `erroOsEmailCliente` ja vem com um endereco sintetico de massa
(`teste.processamento@example.com`); nao ha bloqueio de e-mail antes do login.

| Passo | Resultado esperado |
| --- | --- |
| Login GESTOR | 200 e token valido |
| Criar OS completa sem servicos/pecas | 201, status RECEBIDA, lista de itens vazia |
| Finalizar essa OS | 409, transicao de status invalida |
| Gerar orcamento sem itens | 422, OS sem itens |
| Consultar a OS | 200, ainda RECEBIDA e sem itens |

Cada execucao cria um cliente, um veiculo e uma OS. A OS permanece cadastrada
e participa das contagens de negocio. Os cenarios de erro nao acionam
notificacao: as validacoes interrompem o processamento antes dessa etapa.
A criacao da massa segue o envio normal de notificacao da API. O endereco
sintetico nao comprova entrega SMTP: se tambem quiser validar entrega, troque
`erroOsEmailCliente` por sua caixa real de testes. Os cenarios 409/422 nao
dependem da entrega; eventuais falhas de SMTP na criacao sao registradas
separadamente pela API. Nao se injeta erro de autenticacao ou pagamento.

No Bruno, reimporte a collection corrigida ou atualize a pasta convertida.
O login nao deve conter a linha que lanca `Configure erroOsEmailCliente`.
As leituras de `erroOsId` nos passos 02, 03 e 04 devem usar
`bru.getCollectionVar('erroOsId')`, correspondente a
`pm.collectionVariables.get('erroOsId')` no Postman. `bru.getVar` consulta
variaveis de runtime e nao deve ser usado para ler o ID salvo na collection.
A mesma regra vale para `hpaTokenGestor` na pasta de carga.

Validacao em 15/10/2026: conversor oficial `@usebruno/converters` 0.24.0,
Bruno CLI 4.2.1 e API Spring Boot real local, com H2 e e-mail em modo LOG.
As oito requisicoes das duas pastas passaram, incluindo os testes de resposta.
Na pasta de falhas, a sequencia foi 200, 201, 409, 422 e 200. A execucao local
nao valida SMTP, ingestao New Relic ou escala do Kubernetes.

As respostas 409/422 sao rejeicoes de negocio que a implementacao atual
contabiliza como `FALHA_PROCESSAMENTO`. Nao representam falhas internas 500.
Os scripts validam o codigo e a mensagem, impedindo que 401, 403 ou 500 sejam
aceitos como o resultado esperado.

No ambiente instrumentado, consulte no New Relic (com o filtro do ambiente):

```sql
FROM OficinaOsEvent
SELECT count(*)
WHERE evento = 'FALHA_PROCESSAMENTO'
  AND app_name = 'oficina-dgcar-api-homolog'
FACET fluxo, erro
SINCE 15 minutes ago
```

Confira os fluxos `finalizar_servico` e `gerar_orcamento`.
Use `erroOsCorrelation409` e `erroOsCorrelation422`, salvos na collection,
para pesquisar o campo `correlationId` nos logs e nas transacoes APM.
Os testes HTTP nao validam automaticamente a ingestao no New Relic nem
garantem abertura de alerta: isso depende da coleta e dos limites do alerta.
