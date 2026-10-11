# Automate-it Mini

Aplicação para cadastro de casos de teste, controle de execuções e consulta de resultados, com autenticação, permissões e segregação por projeto.

## Configuração segura

Crie um arquivo local `.env` a partir do exemplo e substitua todas as senhas. O arquivo `.env` é ignorado pelo Git e nunca deve ser enviado ao repositório:

```bash
cp .env.example .env
```

Use senhas longas e diferentes para o PostgreSQL e para o administrador. Não reutilize senhas pessoais.

## Executar localmente para desenvolvimento

```bash
docker compose up -d postgres
cd backend/backend
set -a
source ../../.env
set +a
./mvnw spring-boot:run
```

Em outro terminal, execute o Vite (o proxy existe apenas para desenvolvimento):

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. No primeiro início, a aplicação cria um projeto padrão e a conta administrativa informada pelas variáveis de ambiente. Nenhuma senha padrão é exibida ou armazenada no código.

As variáveis de bootstrap criam a primeira conta administrativa, mas não sobrescrevem alterações feitas depois pela tela de administração. Em uma recuperação emergencial, defina temporariamente `FORCE_ADMIN_RECOVERY=true`, informe `ADMIN_PASSWORD`, reinicie o backend e volte a variável para `false`. A senha precisa ter de 12 a 72 bytes, com letra maiúscula, minúscula e número.

## Imagem completa

O contexto do build precisa ser a raiz do repositório. A imagem compila o React, copia `frontend/dist` para os recursos estáticos do Spring Boot, executa os testes do backend com Java 21 e inicia a aplicação com um usuário não privilegiado:

```bash
docker build -f backend/backend/Dockerfile -t automate-it-mini:full .
```

Ao iniciar a imagem, informe `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` e `ADMIN_DISPLAY_NAME`. Em produção, defina também `SESSION_COOKIE_SECURE=true`. Não coloque valores reais no Dockerfile, no repositório ou na linha de comando compartilhada.

Na imagem completa, React e API usam a mesma origem: `/` entrega a interface e `/api/**` entrega a API. `VITE_API_URL` não é necessária em produção.

## Permissões

| Papel | Leitura | Gerenciar casos de teste | Executar e registrar resultados | Editar/excluir execuções | Administrar usuários e projetos |
| --- | --- | --- | --- | --- | --- |
| `ADMIN` | Todos os projetos | Sim | Sim | Sim | Sim |
| `MANAGER` | Projetos atribuídos | Sim | Sim | Sim | Não |
| `TESTER` | Projetos atribuídos | Não | Sim | Não | Não |
| `VIEWER` | Projetos atribuídos | Não | Não | Não | Não |

Os dados de Test Cases, Test Runs e Results são sempre consultados dentro do projeto ativo. A API valida novamente o vínculo do usuário com o projeto; ocultar botões no frontend não é a única proteção.

## Segurança

- Usuários e vínculos com projetos são persistidos no PostgreSQL.
- Senhas são armazenadas com BCrypt.
- Após cinco tentativas incorretas, a conta fica bloqueada por 15 minutos.
- A autenticação usa sessão HTTP com cookie `HttpOnly`.
- Operações de escrita exigem token CSRF; o cookie CSRF também é `HttpOnly` e o token é obtido pelo corpo de `/api/auth/csrf`.
- CORS aceita apenas as origens configuradas em `CORS_ALLOWED_ORIGINS`.
- A sessão expira após 8 horas de inatividade.
- O Swagger e o contrato OpenAPI exigem autenticação de administrador.

## Configuração no Render

Use um Web Service Docker com estas opções:

| Campo | Valor |
| --- | --- |
| Dockerfile Path | `backend/backend/Dockerfile` |
| Docker Build Context Directory | `.` (raiz do repositório) |
| Health Check Path | `/api/auth/csrf` |

Cadastre como variáveis secretas `DB_URL` (URL JDBC do Neon), `DB_USERNAME`, `DB_PASSWORD` e `ADMIN_PASSWORD`. Cadastre ainda:

```text
ADMIN_USERNAME=admin
ADMIN_DISPLAY_NAME=Administrator
SESSION_COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://automate-it-mini.onrender.com
HIBERNATE_DDL_AUTO=update
```

Mantenha `FORCE_ADMIN_RECOVERY=false` (ou não cadastre essa variável) durante a operação normal, para que redefinições de senha feitas no sistema persistam após novos deploys.

O Render injeta `PORT`; a aplicação o utiliza automaticamente. Mantenha `HIBERNATE_DDL_AUTO=update` nesta publicação para preservar o esquema atual. Uma migração futura para Flyway deve ser preparada e validada antes de trocar esse valor por `validate`.

## Validação

```bash
cd backend/backend && ./mvnw clean verify
cd frontend && npm ci && npm run lint && npm run build
```
