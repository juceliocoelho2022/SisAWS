# Engineering Decisions — SisAWS

Este documento separa o que está implementado, o que está em evolução e quais decisões orientam o produto.

## 1. Problema

O SisAWS é uma plataforma de estudo para certificações AWS. O desafio não é apenas exibir perguntas, mas transformar desempenho em feedback útil sem criar arquitetura mais complexa do que o produto precisa.

## 2. Spring Boot + React

**Decisão:** backend REST em Java/Spring Boot e frontend React/TypeScript.

**Benefício:** separação entre interface, autenticação, regras e persistência.

**Trade-off:** dois builds e um contrato HTTP para manter.

## 3. Recomendação determinística em vez de IA artificial

A revisão inteligente atual usa taxa de acerto, volume de respostas e reincidência de erros.

**Decisão:** não usar ML sem dados e hipótese que justifiquem o custo.

**Benefício:** regra explicável, testável e adequada ao estágio atual.

**Gatilho para ML:** dados suficientes, hipótese mensurável e baseline determinístico para comparação.

## 4. JWT stateless

Spring Security + JWT permitem API sem sessão em memória no servidor. Senhas usam BCrypt.

**Trade-off:** revogação e rotação de tokens precisam de estratégia própria em uma evolução de produção.

## 5. H2 e PostgreSQL

H2 acelera o ciclo local; PostgreSQL representa o ambiente persistente.

**Risco:** diferenças entre bancos podem mascarar incompatibilidades.

**Mitigação:** migrations, queries e cenários críticos devem ser validados no PostgreSQL.

## 6. Terraform e AWS

A arquitetura declarada inclui VPC, ALB, ECS/Fargate, ECR, RDS, S3/CloudFront, CloudWatch e Secrets Manager.

**Importante:** infraestrutura planejada ou parcialmente criada não deve ser apresentada como produção concluída sem validação do deploy.

IaC existe para tornar infraestrutura reproduzível e revisável, mas adiciona estado, custo e responsabilidade de segurança.

## 7. Segurança e custo cloud

Antes de chamar o ambiente de production-like:
- secrets fora do código;
- IAM com menor privilégio;
- banco em subnets privadas;
- security groups restritivos;
- HTTPS;
- métricas e logs;
- budgets/alerts;
- backup.

## 8. Estratégia de testes

Prioridades:
- cálculo do resultado;
- isolamento por usuário;
- autenticação/autorização;
- caderno de erros;
- recomendação determinística;
- contrato REST;
- persistência em PostgreSQL.

## 9. Diagnóstico de percentual incorreto

1. Reproduzir com conjunto conhecido.
2. Conferir payload do frontend.
3. Validar contabilização de não respondidas.
4. Comparar total de questões e respostas.
5. Verificar persistência da tentativa.
6. Criar teste que reproduza o bug.
7. Corrigir e impedir regressão.

## 10. Evolução por necessidade

ML, cache, mensageria e microsserviços só entram quando métricas ou requisitos demonstrarem necessidade real.
