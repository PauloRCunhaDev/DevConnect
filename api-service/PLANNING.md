# DevConnect — Planejamento do Projeto

> Plataforma de networking para desenvolvedores  
> Stack: Java 21 · Spring Boot 3 · PostgreSQL · Docker  
> Objetivo: cobrir todos os níveis de backend development para portfólio

---

## Visão geral

DevConnect é uma plataforma onde desenvolvedores criam perfis, publicam posts técnicos, se conectam via chat em tempo real e recebem notificações. O projeto é estruturado em 4 fases progressivas, cada uma introduzindo um novo conjunto de conceitos de backend.

---

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 3.x |
| Banco de dados | PostgreSQL 16 |
| Cache | Redis 7 (Fase 4) |
| Message broker | RabbitMQ ou Redis Streams (Fase 4) |
| Containerização | Docker + Docker Compose |
| Autenticação | Spring Security + JWT + OAuth2 |
| Build tool | Maven ou Gradle |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Docs de API | SpringDoc OpenAPI (Swagger UI) |

---

## Arquitetura geral

```
devconnect/
├── src/
│   └── main/
│       ├── java/com/devconnect/
│       │   ├── config/          # Configurações (Security, Redis, etc)
│       │   ├── controller/      # REST Controllers
│       │   ├── service/         # Regras de negócio
│       │   ├── repository/      # JPA Repositories
│       │   ├── model/           # Entidades JPA
│       │   ├── dto/             # Data Transfer Objects
│       │   ├── exception/       # Exceções customizadas
│       │   └── util/            # Utilitários
│       └── resources/
│           ├── application.yml
│           └── db/migration/    # Flyway migrations
├── docker-compose.yml
├── Dockerfile
├── PLANNING.md
└── AGENTS.md
```

---

## Fase 1 — Autenticação e CRUD básico

**Conceitos cobertos:** JWT Authentication API · Notes/Todo CRUD API · Comment System · URL Redirect Service

### Funcionalidades
- Registro de usuário com email e senha
- Login retornando access token (JWT) e refresh token
- CRUD completo de posts técnicos (título, conteúdo, tags)
- Sistema de comentários em posts
- Perfil público com URL personalizada (ex: `/u/joaodev`)

### Entidades principais
```
User
- id (UUID)
- name
- email (unique)
- passwordHash
- username (unique) ← usado na URL do perfil
- bio
- createdAt

Post
- id (UUID)
- authorId (FK → User)
- title
- content
- tags (array)
- createdAt
- updatedAt

Comment
- id (UUID)
- postId (FK → Post)
- authorId (FK → User)
- content
- createdAt
```

### Endpoints principais
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh

GET    /api/users/{username}
PUT    /api/users/me

GET    /api/posts
POST   /api/posts
GET    /api/posts/{id}
PUT    /api/posts/{id}
DELETE /api/posts/{id}

GET    /api/posts/{postId}/comments
POST   /api/posts/{postId}/comments
DELETE /api/posts/{postId}/comments/{id}

GET    /u/{username}  ← redirect para perfil público
```

### Conceitos Java/Spring a aprender
- `@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestBody`
- Spring Security filter chain para proteger rotas
- `UserDetailsService` para autenticação
- Geração e validação de JWT com `jjwt`
- `JpaRepository` e relacionamentos `@OneToMany`, `@ManyToOne`
- `@ControllerAdvice` para tratamento global de erros
- DTOs com records Java + validação com `@Valid`
- Flyway para migrations do banco

### Critérios de conclusão
- [ ] Registro e login funcionando com JWT
- [ ] CRUD de posts protegido (apenas autor pode editar/deletar)
- [ ] Comentários funcionando
- [ ] URL de perfil redirecionando corretamente
- [ ] Testes unitários dos services principais

---

## Fase 2 — Busca, uploads e proteção

**Conceitos cobertos:** Search + Filter API · Image/File Storage API · API Rate Limiter · Real-time Chat (WebSocket)

### Funcionalidades
- Busca de posts por título, tags e autor
- Filtros combinados com paginação
- Upload de avatar de perfil
- Rate limiting por IP e por usuário autenticado
- Chat em tempo real entre desenvolvedores

### Adições ao modelo
```
Follow
- followerId (FK → User)
- followingId (FK → User)
- createdAt

Message
- id (UUID)
- senderId (FK → User)
- receiverId (FK → User)
- content
- sentAt
- read (boolean)
```

### Endpoints novos
```
GET  /api/posts?q=spring&tags=java,backend&page=0&size=20
GET  /api/users?stack=java&location=br

POST /api/users/me/avatar   ← upload multipart
GET  /api/users/{username}/avatar

POST /api/follows/{username}
DELETE /api/follows/{username}

WS   /ws/chat  ← WebSocket STOMP
GET  /api/messages/{username}
```

### Conceitos Java/Spring a aprender
- `Pageable` e `Page<T>` para paginação
- Queries com `@Query` JPQL ou Specifications
- `MultipartFile` para upload de arquivos
- Armazenamento local de arquivos (ou S3-compatible com MinIO no Docker)
- `bucket4j` ou `resilience4j` para rate limiting
- Spring WebSocket com STOMP
- `SimpMessagingTemplate` para enviar mensagens

### Critérios de conclusão
- [ ] Busca com filtros e paginação funcionando
- [ ] Upload de avatar funcionando
- [ ] Rate limiting bloqueando abuso (teste com curl em loop)
- [ ] Chat básico via WebSocket funcionando
- [ ] Testes de integração com Testcontainers

---

## Fase 3 — Permissões e login social

**Conceitos cobertos:** Role-Based Access Control · OAuth2 / Social Login · Background Jobs

### Funcionalidades
- Papéis de usuário: `USER`, `MODERATOR`, `ADMIN`
- Admins podem banir usuários e remover posts
- Moderadores podem ocultar comentários
- Login com conta GitHub (OAuth2)
- Jobs em background para processar eventos assíncronos

### Adições ao modelo
```
User
+ role (enum: USER, MODERATOR, ADMIN)
+ banned (boolean)
+ oauthProvider (nullable)
+ oauthId (nullable)

AuditLog
- id (UUID)
- actorId (FK → User)
- action (string)
- targetType (string)
- targetId (string)
- createdAt
```

### Endpoints novos
```
POST   /api/admin/users/{id}/ban
DELETE /api/admin/posts/{id}
PUT    /api/admin/users/{id}/role

POST   /api/moderator/comments/{id}/hide

GET    /oauth2/authorization/github  ← inicia fluxo OAuth2
GET    /login/oauth2/code/github     ← callback do GitHub
```

### Conceitos Java/Spring a aprender
- `@PreAuthorize("hasRole('ADMIN')")` e SpEL
- `GrantedAuthority` e hierarquia de roles
- `spring-security-oauth2-client` para GitHub login
- `OAuth2UserService` para mapear perfil GitHub → User local
- `@Async` com `ThreadPoolTaskExecutor` para jobs simples
- `@Scheduled` para tarefas periódicas (limpeza de tokens expirados)
- Audit log com `@EntityListeners` ou Spring AOP

### Critérios de conclusão
- [ ] Login com GitHub funcionando
- [ ] Rotas de admin protegidas por role
- [ ] Moderadores só conseguem moderar, não banir
- [ ] Jobs assíncronos rodando sem bloquear a request
- [ ] Audit log registrando ações administrativas

---

## Fase 4 — Escala e arquitetura avançada

**Conceitos cobertos:** Redis Caching · Background Job/Queue System · Notification Service · Event-Driven Microservices

### Funcionalidades
- Cache do feed e perfis populares com Redis
- Fila de mensagens para processar notificações
- Notificações por email (SMTP) e push via WebSocket
- Serviço de notificação desacoplado via eventos
- Métricas e health checks com Actuator

### Novos componentes
```
Notification
- id (UUID)
- userId (FK → User)
- type (COMMENT, FOLLOW, MENTION)
- referenceId (string)
- read (boolean)
- createdAt

Eventos publicados:
- UserFollowed
- PostCommented
- UserMentioned
```

### Endpoints novos
```
GET  /api/notifications
PUT  /api/notifications/{id}/read
PUT  /api/notifications/read-all

GET  /actuator/health
GET  /actuator/metrics
```

### Conceitos Java/Spring a aprender
- `spring-boot-starter-data-redis` com `@Cacheable`, `@CacheEvict`
- `RedisTemplate` para operações manuais
- `spring-rabbit` ou Redis Streams como message broker
- `JavaMailSender` para envio de email
- `ApplicationEventPublisher` (eventos internos) vs broker externo
- Padrão de design: Event-driven com producers e consumers desacoplados
- `spring-boot-starter-actuator` para observabilidade

### Critérios de conclusão
- [ ] Feed carregando do cache (verificar com logs de query)
- [ ] Notificação de comentário chegando em tempo real
- [ ] Email de boas-vindas sendo enviado após registro
- [ ] Producer e consumer de eventos em classes separadas
- [ ] Health check respondendo em `/actuator/health`

---

## Docker Compose (desenvolvimento)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: devconnect
      POSTGRES_USER: devconnect
      POSTGRES_PASSWORD: devconnect
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"  # dashboard
```

---

## Convenções do projeto

- **Branches:** `main` (produção) → `develop` → `feature/fase-1-jwt-auth`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`)
- **Testes:** toda service class deve ter testes unitários; endpoints críticos com testes de integração
- **Erros:** sempre retornar `{ "error": "mensagem", "status": 400 }` — nunca stacktrace em produção
- **Segurança:** senhas com `BCryptPasswordEncoder`, JWTs com expiração curta (15min) + refresh token (7 dias)

---

## Ordem de estudo recomendada

1. Spring Boot basics + REST + JPA (Fase 1)
2. Spring Security + JWT (Fase 1)
3. Paginação + Queries avançadas (Fase 2)
4. WebSocket com STOMP (Fase 2)
5. OAuth2 + Roles (Fase 3)
6. Redis + Caching (Fase 4)
7. Mensageria + Event-driven (Fase 4)
