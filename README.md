# oficina-dgcar-api

Repositorio da aplicacao principal Spring Boot da Oficina Mecanica DGCar no Tech Challenge 3.

## Proposito

Este repositorio concentra a API principal da oficina, executada em Kubernetes e integrada ao banco PostgreSQL gerenciado.

Foram mantidas as responsabilidades de negocio da aplicacao e foi implementada a validacao de dois tipos de JWT:

- JWT interno de funcionarios, usado por `ATENDENTE`, `MECANICO` e `GESTOR`.
- JWT externo de cliente, emitido pela Lambda `oficina-dgcar-auth-lambda` com `tipo=CLIENTE`.

A validacao do JWT externo foi implementada tambem na aplicacao para defesa em profundidade, alem da protecao no API Gateway.

## Tecnologias

- Java 17
- Spring Boot
- Spring Security
- Maven
- PostgreSQL
- Flyway
- Docker
- Kubernetes
- GitHub Actions
- OpenAPI/Swagger

## Relacao Com Os Demais Repositorios

- `oficina-dgcar-auth-lambda`: emite JWT externo de cliente no fluxo `POST /auth/cpf`.
- `oficina-dgcar-infra-db`: provisiona o RDS PostgreSQL consumido pela aplicacao.
- `oficina-dgcar-infra-k8s`: provisiona EKS, ECR, API Gateway e integra o Gateway com a aplicacao.
- `mvp-posfiap-oficina-mecanica`: permanece como repositorio historico da evolucao.

## Arquitetura De Entrada

O API Gateway foi definido como entrada oficial da solucao.

Fluxo de autenticacao externa:

```text
Cliente -> API Gateway POST /auth/cpf -> Lambda Auth CPF -> RDS PostgreSQL -> JWT CLIENTE
```

Fluxo de consumo protegido:

```text
Cliente -> API Gateway -> API Spring Boot no EKS -> Validacao JWT CLIENTE -> Regras por cliente
```

Endpoint de homologacao do API Gateway:

```text
https://vqgo7dwgqj.execute-api.us-east-1.amazonaws.com
```

Rota publica de autenticacao por CPF:

```http
POST /auth/cpf
Content-Type: application/json

{
  "cpf": "12345678909"
}
```

Resposta esperada quando o cliente existe e esta apto:

```json
{
  "token": "<jwt-cliente>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## JWT Externo De Cliente

O JWT externo foi separado do JWT interno de funcionarios.

Claims esperadas no JWT de cliente:

```json
{
  "sub": "12345678909",
  "clienteId": 1,
  "tipo": "CLIENTE",
  "status": "ATIVO",
  "iat": 1760000000,
  "exp": 1760003600,
  "iss": "oficina-dgcar-auth-lambda",
  "aud": "oficina-dgcar-api"
}
```

Validacoes implementadas na API:

- Assinatura com `CLIENT_JWT_SECRET`.
- `issuer` igual a `CLIENT_JWT_ISSUER`.
- `audience` igual a `CLIENT_JWT_AUDIENCE`.
- Claim `tipo=CLIENTE`.
- Claim `clienteId` para restricao de acesso por cliente.
- Claim `sub` com CPF para restricao na abertura completa de OS.

## Rotas Publicas

As rotas publicas permanecem sem JWT da aplicacao:

| Metodo | Rota | Finalidade |
|---|---|---|
| `POST` | `/auth/login` | Login interno de funcionario |
| `POST` | `/auth/registrar` | Registro interno conforme regra atual |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/api-docs/**` | OpenAPI |
| `GET` | `/actuator/health/**` | Healthcheck |
| `GET` | `/actuator/info` | Informacoes basicas |
| `GET` | `/api/ordens-servico/status/{numero}` | Consulta publica de status por numero |
| `GET` | `/api/ordens-servico/orcamento/decisao/{token}` | Consulta de decisao por token opaco |
| `POST` | `/api/ordens-servico/orcamento/decisao/{token}/aprovar` | Aprovacao por token opaco |
| `POST` | `/api/ordens-servico/orcamento/decisao/{token}/recusar` | Recusa por token opaco |

No API Gateway, a rota publica externa de autenticacao por CPF foi criada no repositorio `oficina-dgcar-infra-k8s`:

| Metodo | Rota | Integracao |
|---|---|---|
| `POST` | `/auth/cpf` | Lambda `oficina-dgcar-auth-lambda` |

## Rotas Protegidas Por JWT De Cliente

As rotas abaixo aceitam JWT externo `CLIENTE` emitido pela Lambda, alem dos perfis internos autorizados:

| Metodo | Rota | Regra implementada |
|---|---|---|
| `POST` | `/api/ordens-servico/completa` | `CLIENTE` pode abrir OS apenas quando o CPF do corpo e igual ao `sub` do JWT |
| `GET` | `/api/ordens-servico/cliente/{clienteId}` | `CLIENTE` pode listar apenas ordens do proprio `clienteId` |

Exemplo de chamada protegida:

```bash
curl --location "https://vqgo7dwgqj.execute-api.us-east-1.amazonaws.com/api/ordens-servico/cliente/1" \
  --header "Authorization: Bearer <jwt-cliente>"
```

## Rotas Protegidas Por JWT Interno

As demais rotas operacionais permanecem protegidas por JWT interno e roles da oficina:

- `ATENDENTE`
- `MECANICO`
- `GESTOR`

Exemplos:

| Grupo | Perfis |
|---|---|
| Gestao de clientes | `ATENDENTE`, `GESTOR` |
| Gestao de veiculos | `ATENDENTE`, `GESTOR` |
| Criacao e acompanhamento operacional de OS | `ATENDENTE`, `MECANICO`, `GESTOR`, conforme rota |
| Catalogo de pecas e servicos | `GESTOR` ou perfis ja definidos no `SecurityConfig` |
| Metricas internas | JWT interno conforme configuracao da aplicacao |

## Variaveis De Ambiente

Variaveis obrigatorias para execucao em nuvem:

| Nome | Finalidade |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL gerenciado |
| `SPRING_DATASOURCE_USERNAME` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo do JWT interno de funcionarios |
| `CLIENT_JWT_SECRET` | Segredo do JWT externo de clientes, igual ao usado pela Lambda |
| `CLIENT_JWT_ISSUER` | Issuer esperado para JWT externo, padrao `oficina-dgcar-auth-lambda` |
| `CLIENT_JWT_AUDIENCE` | Audience esperada para JWT externo, padrao `oficina-dgcar-api` |
| `NEW_RELIC_LICENSE_KEY` | Chave do New Relic quando observabilidade estiver habilitada |
| `SMTP_USERNAME` | Usuario SMTP quando envio real estiver habilitado |
| `SMTP_PASSWORD` | Senha SMTP quando envio real estiver habilitado |

Secrets esperados no GitHub:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `CLIENT_JWT_SECRET`
- `NEW_RELIC_LICENSE_KEY`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`

## Execucao Local

Subir dependencias locais:

```bash
docker compose up -d
```

Executar a aplicacao:

```bash
mvn spring-boot:run
```

Executar testes:

```bash
mvn test
```

## Deploy Manual Em Kubernetes

Foi implementado deploy manual no workflow `App CI/CD - Build, Test and Deploy`.

O botao `Run workflow` fica disponivel no GitHub Actions apos o merge da alteracao que adiciona `workflow_dispatch`.

Fluxo executado pelo workflow:

1. Executa `mvn clean test`.
2. Gera imagem Docker.
3. Publica a imagem no Amazon ECR.
4. Atualiza o kubeconfig do EKS.
5. Cria ou atualiza namespace, ConfigMap, Secret, Deployment, Service e HPA.
6. Aguarda o rollout do deployment.
7. Exibe pods, service, HPA e URL do LoadBalancer.

Execucao pelo GitHub:

```text
Actions -> App CI/CD - Build, Test and Deploy -> Run workflow
```

Inputs:

| Campo | Valor |
|---|---|
| `environment` | `homolog` ou `prod` |

O deploy real foi mantido apenas via execucao manual e usa GitHub Environments. Assim, `homolog` e `prod` continuam exigindo aprovacao antes da execucao do job de deploy.

Secrets obrigatorios no environment escolhido:

| Nome | Origem |
|---|---|
| `AWS_ACCESS_KEY_ID` | Usuario IAM usado pelo GitHub Actions |
| `AWS_SECRET_ACCESS_KEY` | Usuario IAM usado pelo GitHub Actions |
| `SPRING_DATASOURCE_USERNAME` | Usuario do RDS PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Senha do RDS PostgreSQL |
| `JWT_SECRET` | Segredo do JWT interno |
| `CLIENT_JWT_SECRET` | Mesmo segredo usado pela Lambda Auth CPF |
| `SMTP_USERNAME` | Usuario SMTP, pode ficar vazio em homolog quando e-mail esta em modo LOG |
| `SMTP_PASSWORD` | Senha SMTP, pode ficar vazio em homolog quando e-mail esta em modo LOG |

Variaveis obrigatorias no environment escolhido:

| Nome | Origem |
|---|---|
| `AWS_REGION` | Regiao AWS, por exemplo `us-east-1` |
| `ECR_REPOSITORY` | Nome ou URL do repositorio ECR da aplicacao |
| `EKS_CLUSTER_NAME` | Nome do cluster EKS |
| `SPRING_DATASOURCE_URL` | Output `spring_datasource_url` do repo `oficina-dgcar-infra-db` |

O workflow tambem aceita `AWS_REGION`, `ECR_REPOSITORY`, `EKS_CLUSTER_NAME` e `SPRING_DATASOURCE_URL` como Secrets. Quando os dois existem, o valor em Secret tem prioridade.

Origem dos valores:

| Valor no repo `oficina-dgcar-api` | Origem operacional |
|---|---|
| `AWS_REGION` | Regiao usada pelos repos de infraestrutura, atualmente `us-east-1` |
| `ECR_REPOSITORY` | Output `ecr_repository_url` do repo `oficina-dgcar-infra-k8s`, ou apenas o nome do ECR se preferir |
| `EKS_CLUSTER_NAME` | Output `eks_cluster_name` do repo `oficina-dgcar-infra-k8s` |
| `SPRING_DATASOURCE_URL` | Output `spring_datasource_url` do repo `oficina-dgcar-infra-db` |
| `SPRING_DATASOURCE_USERNAME` | Mesmo valor usado como `DB_USERNAME`/usuario do RDS no repo `oficina-dgcar-infra-db` |
| `SPRING_DATASOURCE_PASSWORD` | Mesmo valor usado como `DB_PASSWORD`/senha do RDS no repo `oficina-dgcar-infra-db` |
| `CLIENT_JWT_SECRET` | Mesmo valor configurado no environment do repo `oficina-dgcar-auth-lambda` |
| `JWT_SECRET` | Segredo interno da API; pode reaproveitar o valor do repositorio historico quando ainda estiver valido |

Comandos para consultar os outputs no GitHub Actions:

```bash
# No repo oficina-dgcar-infra-k8s, apos o apply de homolog:
terraform output ecr_repository_url
terraform output eks_cluster_name

# No repo oficina-dgcar-infra-db, apos o apply de homolog:
terraform output spring_datasource_url
```

Variaveis opcionais no environment escolhido:

| Nome | Padrao |
|---|---|
| `CLIENT_JWT_ISSUER` | `oficina-dgcar-auth-lambda` |
| `CLIENT_JWT_AUDIENCE` | `oficina-dgcar-api` |
| `OFICINA_EMAIL_ENABLED` | `false` |
| `OFICINA_EMAIL_MODE` | `LOG` |
| `OFICINA_EMAIL_REMETENTE` | `no-reply@dgcar.local` |
| `SMTP_HOST` | `localhost` |
| `SMTP_PORT` | `587` |
| `SMTP_AUTH` | `true` |
| `SMTP_STARTTLS_ENABLE` | `true` |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | `false` |
| `PAGAMENTO_GATEWAY_APPROVAL_RATE` | `1.0` |
| `PAGAMENTO_GATEWAY_LATENCY_MS` | `0` |

Validacao apos deploy:

```bash
kubectl get pods -n oficina
kubectl logs -n oficina deployment/oficina-api
kubectl get svc oficina-api -n oficina
```

O deploy da API tambem executa as migrations Flyway na inicializacao da aplicacao. A Lambda Auth CPF depende dessas tabelas e dados para retornar JWT no endpoint `POST /auth/cpf`.

## Validacoes Realizadas

Foi executada a suite completa de testes:

```text
Tests run: 366, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

Foram adicionadas validacoes automatizadas para:

- JWT externo `CLIENTE` com issuer e audience validos.
- Rejeicao de JWT externo com audience invalida.
- Autenticacao no filtro Spring Security com role `CLIENTE`.
- Propagacao de `clienteId` nos detalhes de autenticacao.
- Separacao entre JWT interno de funcionarios e JWT externo de cliente.

## Branches E Ambientes

- `main`: producao, protegida e sem commits diretos.
- `homolog`: homologacao.
- Pull Requests foram mantidos como caminho obrigatorio de merge.
- GitHub Environments `homolog` e `prod` foram configurados para aprovacao manual antes de deploy.

## Status Da Implementacao

Foi implementada a validacao do JWT externo de cliente na API Spring Boot.

Foi documentada a classificacao das rotas publicas, rotas protegidas por JWT de cliente e rotas protegidas por JWT interno.

O proximo passo operacional e publicar esta alteracao no GitHub por Pull Request, aprovar o merge e executar o deploy da aplicacao no EKS para conectar o API Gateway tambem as rotas da API principal.

## Origem Historica

A extracao inicial foi realizada a partir do repositorio historico.

Artefatos extraidos:

- `src/**`
- `pom.xml`
- `Dockerfile`
- `docker-compose.yml`
- `api-requests.http`
- `postman/**`
- `allure-report.ps1`
- `docs/ReportOWASP/**`
- `docs/ReportTRIVY/**`
- `.github/workflows/app-cd.yml`

O commit de origem esta registrado em [`ORIGEM_HISTORICA.md`](./ORIGEM_HISTORICA.md).
