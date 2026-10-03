# oficina-dgcar-api

Repositório da aplicação principal Spring Boot da Oficina Mecânica DGCar no Tech Challenge 3.

## Propósito

- Manter a API principal Spring Boot.
- Executar testes automatizados.
- Gerar imagem Docker.
- Publicar imagem no ECR.
- Implantar a aplicação no Kubernetes.
- Validar JWT interno de funcionários e JWT externo de clientes.
- Expor Swagger/OpenAPI e artefatos Postman.

## Tecnologia Alvo

- Java 17
- Spring Boot
- Maven
- Docker
- PostgreSQL
- Kubernetes
- GitHub Actions

## Branches E Ambientes

- `main`: produção, protegida e sem commits diretos.
- `homolog`: homologação, com deploy automático quando configurado.
- GitHub Environments esperados: `homolog` e `prod`.

## Secrets Esperados

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `ECR_REPOSITORY`
- `EKS_CLUSTER_NAME`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `CLIENT_JWT_SECRET`
- `NEW_RELIC_LICENSE_KEY`
- `SMTP_USERNAME`, se envio real de e-mails estiver habilitado
- `SMTP_PASSWORD`, se envio real de e-mails estiver habilitado

## Relação Com Os Demais Repositórios

- Consome banco provisionado por `oficina-dgcar-infra-db`.
- Consome cluster/ECR/API Gateway provisionados por `oficina-dgcar-infra-k8s`.
- Valida tokens externos emitidos por `oficina-dgcar-auth-lambda`.

## Status

Estrutura inicial criada. Código da aplicação, Dockerfile, Postman, Swagger e pipeline serão extraídos nas próximas etapas.
