# SisAWS v1

Plataforma de estudos e simulados para certificações AWS.

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Web / Data JPA / Validation / Actuator
- H2 para execução local rápida
- PostgreSQL 17 via Docker Compose
- React 19 + TypeScript + Vite
- Docker Compose

## Recursos do MVP

- Dashboard com indicadores de estudo
- Certificação AWS Solutions Architect Associate (SAA-C03)
- Banco inicial de questões originais
- Simulado por domínio/serviço
- Correção automática
- Explicação de cada questão
- Histórico de tentativas
- Backend preparado para PostgreSQL
- Frontend responsivo com tema escuro

> As questões deste projeto são autorais e educacionais. Não são questões reais/vazadas dos exames AWS.

## Estrutura

```text
SisAWS-v1/
├── backend/      # Spring Boot
├── frontend/     # React + TypeScript
├── docker-compose.yml
└── README.md
```

## 1. Executar o backend no IntelliJ

Abra a pasta `backend` como projeto Maven.

Classe principal:

`br.com.sisaws.SisAwsApplication`

O perfil padrão usa H2 em memória.

Backend: `http://localhost:8080`

Health: `http://localhost:8080/actuator/health`

Console H2: `http://localhost:8080/h2-console`

Dados H2:

```text
JDBC URL: jdbc:h2:mem:sisaws
User: sa
Password: (vazio)
```

## 2. Executar frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## 3. Executar com PostgreSQL

Na raiz:

```bash
docker compose up -d postgres
```

Depois execute o backend com:

```text
SPRING_PROFILES_ACTIVE=postgres
```

ou pelo IntelliJ em Environment Variables.

## Endpoints principais

```text
GET  /api/v1/certifications
GET  /api/v1/dashboard
GET  /api/v1/questions?certification=SAA-C03&limit=10
POST /api/v1/simulations/finish
GET  /api/v1/simulations/history
```

## Exemplo de envio de simulado

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

## Próximas versões sugeridas

- v1.1: login + JWT
- v1.2: caderno de erros
- v1.3: flashcards e trilhas
- v1.4: simulados de 65 questões com cronômetro
- v1.5: modo adaptativo
- v2: deploy AWS com S3 + CloudFront + ECS/Fargate + RDS
- v3: Terraform + CI/CD + observabilidade
