# SisAWS

Plataforma full stack para estudos, simulados e evolução técnica em certificações AWS, iniciando pela **AWS Certified Solutions Architect – Associate (SAA-C03)**.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=111827)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-F59E0B)

## Visão geral

O **SisAWS** foi criado para transformar o estudo de AWS em uma experiência prática, mensurável e evolutiva.

A proposta é centralizar:

- simulados por certificação;
- questões por serviço e domínio;
- correção automática;
- explicações de respostas;
- acompanhamento de desempenho por aluno;
- autenticação com Spring Security + JWT;
- caderno de erros automático;
- trilhas de estudo;
- progresso por serviço AWS;
- flashcards;
- recomendações orientadas por desempenho;
- ranking de pontos fracos;
- revisão inteligente;
- prática adaptativa;
- laboratórios práticos;
- histórico de evolução.

O primeiro foco do produto é a **SAA-C03**, com arquitetura preparada para receber novas certificações e novos módulos.

> As questões utilizadas no projeto são autorais e educacionais. O SisAWS não utiliza dumps ou questões vazadas de exames oficiais.

---

## Funcionalidades atuais

### Dashboard

- banco de questões;
- quantidade de simulados realizados;
- média de desempenho;
- melhor pontuação;
- orientação de próxima etapa de estudo.

### Simulados

- carregamento de questões via API REST;
- alternativas de múltipla escolha;
- progresso do simulado;
- correção automática;
- resultado percentual;
- explicação individual das respostas;
- tentativas vinculadas ao aluno autenticado;
- confirmação visual da alternativa selecionada com destaque e check;
- filtro por serviço AWS e dificuldade;
- quantidade configurável de questões.

### Modo Estudo e Modo Prova

**Modo Estudo**

- validação imediata da resposta;
- alternativa correta destacada em verde;
- resposta incorreta destacada em vermelho;
- explicação exibida logo após responder;
- ideal para aprendizagem guiada.

**Modo Prova**

- não revela a correção durante a tentativa;
- cronômetro de 130 minutos;
- marcação de questões para revisão;
- permite finalizar com questões não respondidas;
- questões não respondidas entram como incorretas;
- usa como referência o formato atual do SAA-C03: 65 questões em 130 minutos;
- utiliza apenas o banco autoral disponível no SisAWS, sem simular questões inexistentes.

### Autenticação e progresso

- cadastro de aluno;
- login com e-mail e senha;
- senhas protegidas com BCrypt;
- autenticação stateless com JWT;
- perfil do aluno;
- dashboard individual;
- sessão persistida no frontend.

### Caderno de Erros

- registro automático de questões erradas;
- contador de reincidência por questão;
- serviço AWS e domínio da certificação;
- explicação para revisão;
- data do último erro;
- dados isolados por aluno;
- prática focada diretamente no serviço que precisa de revisão.

### Trilhas, Flashcards e Analytics

- trilhas de estudo organizadas por competência;
- flashcards autorais para revisão rápida;
- progresso persistido por serviço AWS;
- percentual de acertos por serviço;
- classificação de status: começando, revisar, bom e forte;
- recomendação automática do próximo serviço a estudar;
- histórico visual das últimas tentativas;
- simulados filtrados por serviço e dificuldade.

### Revisão Inteligente e Modo Adaptativo

O SisAWS calcula uma prioridade de revisão de 0 a 100 combinando:

- taxa de acerto por serviço;
- quantidade de respostas disponíveis;
- reincidência de erros no Caderno de Erros.

A plataforma gera:

- ranking dos serviços que mais precisam de atenção;
- nível geral de aprendizagem;
- serviço prioritário;
- dificuldade sugerida;
- quantidade sugerida de questões;
- sessão adaptativa iniciada diretamente em Modo Estudo.

A regra é determinística e transparente: o sistema não apresenta a recomendação como inteligência artificial ou previsão probabilística.

### Temas da interface

O aluno pode escolher entre:

- **Light**
- **Moderado**
- **Dark**

A preferência é persistida em `localStorage`.

No desktop, a aplicação utiliza toda a viewport e evita rolagem global. Conteúdos extensos, como questões ou revisões, rolam somente dentro da área necessária.

---

## Arquitetura atual

```text
┌───────────────────────────────────────┐
│              Frontend                 │
│        React + TypeScript + Vite      │
└───────────────────┬───────────────────┘
                    │ HTTP / JSON
                    ▼
┌───────────────────────────────────────┐
│               REST API                │
│       Java 21 + Spring Boot 3.5.5     │
│                                       │
│  Controllers                          │
│       ↓                               │
│  Repositories / JPA                   │
│       ↓                               │
│  Hibernate                            │
└───────────────────┬───────────────────┘
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
┌─────────────────┐   ┌─────────────────┐
│ H2              │   │ PostgreSQL 17   │
│ Desenvolvimento │   │ Docker Compose  │
└─────────────────┘   └─────────────────┘
```

### Evolução planejada em AWS

```text
Route 53
   ↓
CloudFront
   ↓
S3 - Frontend React
   ↓
ALB / API Gateway
   ↓
ECS Fargate
   ↓
Spring Boot
   ↓
RDS PostgreSQL

Observabilidade → CloudWatch
Segredos        → Secrets Manager
Imagens         → ECR
IaC             → Terraform
CI/CD           → GitHub Actions
```

---

## Tech Stack

### Backend

| Tecnologia | Uso |
|---|---|
| Java 21 | linguagem principal |
| Spring Boot 3.5.5 | framework backend |
| Spring Web | API REST |
| Spring Data JPA | persistência |
| Hibernate | ORM |
| Spring Security | autenticação e autorização |
| JWT / JJWT | tokens stateless |
| BCrypt | proteção de senhas |
| Bean Validation | validação |
| Spring Boot Actuator | health e observabilidade básica |
| Maven | build e dependências |

### Frontend

| Tecnologia | Uso |
|---|---|
| React 19 | construção da interface |
| TypeScript | tipagem e segurança |
| Vite | build e ambiente de desenvolvimento |
| Lucide React | ícones |
| CSS | design system, responsividade e temas |

### Dados

| Tecnologia | Uso |
|---|---|
| H2 | desenvolvimento local persistente |
| PostgreSQL 17 | banco relacional principal |
| JPA / Hibernate | mapeamento objeto-relacional |

### Infraestrutura

| Tecnologia | Uso |
|---|---|
| Docker | containers |
| Docker Compose | ambiente PostgreSQL local |
| Git | controle de versão |
| GitHub | repositório e colaboração |

### AWS no roadmap

- IAM
- VPC
- EC2
- S3
- CloudFront
- Route 53
- RDS
- Lambda
- API Gateway
- SQS
- SNS
- EventBridge
- ECR
- ECS
- Fargate
- CloudWatch
- Secrets Manager
- KMS

---

## Estrutura do projeto

```text
SisAWS/
├── backend/
│   ├── src/main/java/br/com/sisaws/
│   │   ├── auth/
│   │   ├── certification/
│   │   ├── dashboard/
│   │   ├── errornotebook/
│   │   ├── learning/
│   │   │   ├── AdaptiveLearningController.java
│   │   │   ├── LearningController.java
│   │   │   └── ServiceProgress.java
│   │   ├── question/
│   │   ├── security/
│   │   ├── simulation/
│   │   └── user/
│   ├── src/main/resources/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── App.tsx
│   │   ├── api.ts
│   │   ├── main.tsx
│   │   └── styles.css
│   └── package.json
│
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

## Executando o projeto

### Pré-requisitos

- Java 21
- Maven
- Node.js
- npm
- Docker Desktop, opcional para PostgreSQL
- IntelliJ IDEA, recomendado para o backend

### Backend com H2

Abra:

```text
backend
```

Classe principal:

```text
br.com.sisaws.SisAwsApplication
```

Ou pelo terminal:

```bash
cd backend
mvn spring-boot:run
```

API:

```text
http://localhost:8080
```

Health:

```text
http://localhost:8080/actuator/health
```

H2 Console:

```text
http://localhost:8080/h2-console
```

Credenciais locais:

```text
JDBC URL: jdbc:h2:file:./data/sisaws
User: sa
Password:
```

Os dados locais ficam persistidos no diretório `data/`, que é ignorado pelo Git. Assim, usuários, simulados, analytics e caderno de erros sobrevivem às reinicializações do backend.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface:

```text
http://localhost:5173
```

### PostgreSQL com Docker

Na raiz do projeto:

```bash
docker compose up -d postgres
```

Execute o backend com:

```text
SPRING_PROFILES_ACTIVE=postgres
```

---

## API REST

| Método | Endpoint | Responsabilidade |
|---|---|---|
| POST | `/api/v1/auth/register` | cadastrar aluno |
| POST | `/api/v1/auth/login` | autenticar e emitir JWT |
| GET | `/api/v1/auth/me` | perfil autenticado |
| GET | `/api/v1/certifications` | listar certificações |
| GET | `/api/v1/dashboard` | indicadores individuais |
| GET | `/api/v1/error-notebook` | caderno de erros do aluno |
| GET | `/api/v1/learning/progress` | desempenho por serviço AWS |
| GET | `/api/v1/learning/recommendation` | recomendação de estudo |
| GET | `/api/v1/learning/adaptive/plan` | plano adaptativo e ranking de prioridades |
| GET | `/api/v1/learning/trails` | trilhas AWS |
| GET | `/api/v1/learning/flashcards` | flashcards |
| GET | `/api/v1/questions?certification=SAA-C03&limit=10` | carregar questões |
| GET | `/api/v1/questions?...&service=S3&difficulty=MEDIUM` | simulado filtrado |
| POST | `/api/v1/questions/{id}/check` | validar resposta no Modo Estudo |
| POST | `/api/v1/simulations/finish` | finalizar, corrigir e atualizar analytics |
| GET | `/api/v1/simulations/history` | histórico de tentativas |
| GET | `/actuator/health` | health check |

### Exemplo de finalização

```json
{
  "certificationCode": "SAA-C03",
  "answers": [
    {
      "questionId": 1,
      "selectedOptionIds": [3]
    }
  ]
}
```

---

## Fluxo Git

Depois desta configuração inicial, não é necessário baixar novos ZIPs.

Atualizar o projeto local:

```bash
git pull
```

Verificar alterações:

```bash
git status
```

Salvar alterações locais:

```bash
git add .
git commit -m "feat: descricao da alteracao"
git push
```

---

## Padrão de commits

O projeto utiliza uma convenção baseada em **Conventional Commits**.

| Tipo | Uso |
|---|---|
| `feat` | nova funcionalidade |
| `fix` | correção de bug |
| `docs` | documentação |
| `refactor` | melhoria interna sem alterar comportamento |
| `test` | testes |
| `style` | ajustes visuais ou formatação |
| `perf` | performance |
| `chore` | manutenção e configuração |
| `ci` | pipeline e automação |

Exemplos:

```text
feat(simulation): add timed SAA-C03 exam
fix(question): load answer options with entity graph
docs(readme): professionalize project documentation
refactor(api): move simulation rules to service layer
test(question): add repository integration tests
chore(docker): update PostgreSQL container
ci(github): add backend and frontend pipelines
```

### Histórico inicial

```text
feat: estrutura inicial SisAWS
feat(ui): add theme selector and fullscreen layout
docs(readme): professionalize project documentation
feat(auth): add Spring Security and JWT authentication
feat(progress): link attempts and error notebook to students
feat(web): add student login and error notebook experience
docs(readme): document v1.2 authentication and progress
fix(storage): persist local student progress with file H2
feat(learning): add service analytics trails and flashcards
feat(web): add trails flashcards and learning analytics
docs(readme): document v1.3 learning experience
feat(simulation): confirm selected answer with green check
feat(simulation): add study mode answer checking
feat(exam): add study and exam modes with timer and review flags
docs(readme): document v1.4 exam experience
feat(adaptive): add personalized learning plan engine
feat(web): add smart review and adaptive practice
docs(readme): document v1.5 adaptive learning
```

---

## Roadmap

### v1.1

- [x] dashboard;
- [x] simulados;
- [x] correção automática;
- [x] explicações;
- [x] Light / Moderado / Dark;
- [x] layout desktop fullscreen;
- [x] persistência da preferência de tema.

### v1.2

- [x] cadastro e login;
- [x] Spring Security;
- [x] JWT;
- [x] BCrypt;
- [x] usuários e perfis;
- [x] progresso por aluno;
- [x] histórico de tentativas por usuário;
- [x] caderno de erros automático;
- [x] contador de reincidência de erros.

### v1.3

- [x] flashcards;
- [x] trilhas AWS;
- [x] progresso por serviço;
- [x] filtros por serviço;
- [x] filtros por dificuldade;
- [x] recomendação básica orientada por desempenho;
- [x] histórico visual recente;
- [ ] aulas estruturadas.

### v1.4

- [x] Modo Estudo;
- [x] feedback imediato por resposta;
- [x] Modo Prova;
- [x] cronômetro de 130 minutos;
- [x] marcar questão para revisão;
- [x] finalizar prova com questões não respondidas;
- [x] seleção visual com check;
- [ ] ampliar banco autoral para 65 questões;
- [ ] análise por domínio;
- [ ] histórico detalhado.

### v1.5

- [x] modo adaptativo;
- [x] recomendação com múltiplos sinais;
- [x] ranking pessoal de pontos fracos;
- [x] score de prioridade de 0 a 100;
- [x] revisão inteligente;
- [x] sessão adaptativa em Modo Estudo;
- [x] dificuldade sugerida automaticamente;
- [ ] histórico da evolução do score adaptativo ao longo do tempo.

### v2

- [ ] frontend em S3 + CloudFront;
- [ ] backend em ECS/Fargate;
- [ ] RDS PostgreSQL;
- [ ] ECR;
- [ ] Route 53;
- [ ] CloudWatch;
- [ ] Secrets Manager.

### v3

- [ ] Terraform;
- [ ] GitHub Actions;
- [ ] testes automatizados;
- [ ] observabilidade avançada;
- [ ] arquitetura preparada para múltiplas certificações.

---

## Objetivo técnico

Além de apoiar o estudo para certificações AWS, o SisAWS é um projeto de portfólio para demonstrar competências em:

- Java Backend;
- Spring Boot;
- APIs REST;
- JPA/Hibernate;
- PostgreSQL;
- React;
- TypeScript;
- Docker;
- arquitetura de software;
- Cloud AWS;
- Git/GitHub;
- evolução incremental de produto.

---

## Autor

**Jucelio Farias Coelho**

Desenvolvimento Java Backend • Dados e Engenharia de Dados • Cloud • Testes de Software

GitHub: **@juceliocoelho2022**

---

> **SisAWS — Estude. Pratique. Evolua.**

O SisAWS é um projeto educacional independente e não é afiliado, patrocinado ou endossado pela Amazon Web Services.

Referência do formato do exame: a página oficial da AWS informa atualmente 65 questões e duração de 130 minutos para o AWS Certified Solutions Architect – Associate.
