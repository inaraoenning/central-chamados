# Central de Chamados API

API REST em Kotlin/Spring Boot para abertura e atendimento de chamados de provedores regionais. A aplicação usa PostgreSQL 16, migrations Flyway, JWT, trilha de auditoria append-only e outbox para notificações.

## Requisitos

- Docker e Docker Compose
- `curl` e `jq` para executar o fluxo de exemplo

## Executar com Docker Compose

```bash
docker compose up -d --build
docker compose logs -f api
```

O serviço API fica em <http://localhost:8080>, o Swagger UI em <http://localhost:8080/swagger-ui> e o Mailpit em <http://localhost:8025>. A documentação OpenAPI JSON é publicada em `/v3/api-docs`; uma coleção OpenAPI gerada para importação está em [`openapi.json`](openapi.json). PostgreSQL usa volume persistente `postgres_data`.

Os valores de conexão locais são adequados somente para desenvolvimento. Em outros ambientes, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (mínimo de 32 bytes), `CORS_ALLOWED_ORIGINS` e as variáveis `SMTP_*` no ambiente. Nunca use a chave JWT de desenvolvimento fora do computador local.

## Usuários do seed de desenvolvimento

Todos os usuários abaixo usam a senha `Senha123!`.

| Perfil | E-mail |
| --- | --- |
| ADMIN | `admin@central.local` |
| ATENDENTE — Suporte de Hardware / Infraestrutura | `atendente1@central.local` |
| ATENDENTE — Desenvolvimento / Sistemas | `atendente2@central.local` |
| ATENDENTE — Comercial / Produtos | `atendente3@central.local` |
| ATENDENTE — Suporte Operacional / Administrativo | `atendente4@central.local` |
| SOLICITANTE — Provedor Aurora | `solicitante1@aurora.local`, `solicitante2@aurora.local` |
| SOLICITANTE — Conecta Sul | `solicitante1@conecta.local`, `solicitante2@conecta.local` |

## Fluxo completo com curl

O exemplo abre um chamado no setor Suporte de Hardware / Infraestrutura, assume o chamado, altera prioridade, conclui e consulta as notificações e os logs. As datas são enviadas em UTC.

```bash
BASE=http://localhost:8080/api/v1

SOLICITANTE_TOKEN=$(curl -fsS "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"solicitante1@aurora.local","senha":"Senha123!"}' | jq -r .accessToken)

SETOR_ID=$(curl -fsS "$BASE/setores" -H "Authorization: Bearer $SOLICITANTE_TOKEN" |
  jq -r '.content[] | select(.nome == "Suporte de Hardware / Infraestrutura") | .id')
CATEGORIA_ID=$(curl -fsS "$BASE/setores/$SETOR_ID/categorias" \
  -H "Authorization: Bearer $SOLICITANTE_TOKEN" | jq -r '.content[0].id')

CHAMADO=$(curl -fsS "$BASE/chamados" \
  -H "Authorization: Bearer $SOLICITANTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"setorId\":\"$SETOR_ID\",\"categoriaId\":\"$CATEGORIA_ID\",\"prioridade\":\"MEDIA\",\"titulo\":\"Falha no roteador\",\"descricao\":\"Equipamento reinicia durante o atendimento.\"}")
CHAMADO_ID=$(echo "$CHAMADO" | jq -r .id)
echo "$CHAMADO"

ATENDENTE_TOKEN=$(curl -fsS "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"atendente1@central.local","senha":"Senha123!"}' | jq -r .accessToken)
PREVISAO=$(date -u -d '+1 day' +%Y-%m-%dT%H:%M:%SZ)

curl -fsS "$BASE/chamados/$CHAMADO_ID/assumir" \
  -H "Authorization: Bearer $ATENDENTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"previsaoResolucao\":\"$PREVISAO\"}" | jq

curl -fsS -X PATCH "$BASE/chamados/$CHAMADO_ID/prioridade" \
  -H "Authorization: Bearer $ATENDENTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"prioridade":"ALTA","motivo":"Impacto para o cliente"}' | jq

curl -fsS "$BASE/chamados/$CHAMADO_ID/concluir" \
  -H "Authorization: Bearer $ATENDENTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"observacaoResolucao":"Roteador substituído e conexão validada."}' | jq

curl -fsS "$BASE/notificacoes" \
  -H "Authorization: Bearer $SOLICITANTE_TOKEN" | jq
curl -fsS "$BASE/chamados/$CHAMADO_ID/logs" \
  -H "Authorization: Bearer $ATENDENTE_TOKEN" | jq
```

## Registrar um token push Expo

Autentique o usuário no app e registre o token Expo. O envio assíncrono inclui `chamadoId` e `tipoEvento` em `data` para permitir deep linking.

```bash
curl -fsS -X POST "$BASE/dispositivos" \
  -H "Authorization: Bearer $SOLICITANTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"token":"ExponentPushToken[seu-token]","plataforma":"EXPO"}'
```

Remova o dispositivo com `DELETE /api/v1/dispositivos/{token}`. Preferências de push e e-mail podem ser lidas e atualizadas em `/api/v1/notificacoes/preferencias`. Notificações in-app não podem ser desativadas.

## Desenvolvimento e testes

Com JDK 21 e Docker disponíveis:

```bash
./gradlew build
```

O build executa ktlint, testes unitários e testes de integração com PostgreSQL em Testcontainers. Para executar a aplicação localmente fora do Compose, inicialize um PostgreSQL compatível, configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, e use `SPRING_PROFILES_ACTIVE=dev`.
