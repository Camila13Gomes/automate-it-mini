# Automate-it Mini

Aplicação para cadastro de casos de teste, controle de execuções e consulta de resultados, com autenticação, permissões e segregação por projeto.

## Configuração segura

Crie um arquivo local `.env` a partir do exemplo e substitua todas as senhas. O arquivo `.env` é ignorado pelo Git e nunca deve ser enviado ao repositório:

```bash
cp .env.example .env
```

Use senhas longas e diferentes para o PostgreSQL e para o administrador. Não reutilize senhas pessoais.

## Executar localmente

```bash
docker compose up -d postgres
cd backend/backend
set -a
source ../../.env
set +a
./mvnw spring-boot:run
```

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. No primeiro início, a aplicação cria um projeto padrão e a conta administrativa informada pelas variáveis de ambiente. Nenhuma senha padrão é exibida ou armazenada no código.

As variáveis de bootstrap mantêm a conta administrativa inicial sincronizada a cada início. Definir uma nova `ADMIN_PASSWORD` e reiniciar o backend atualiza a senha dessa conta.

## Permissões

| Papel | Leitura | Criar casos e execuções | Alterar status | Administrar usuários e projetos |
| --- | --- | --- | --- | --- |
| `ADMIN` | Todos os projetos | Sim | Sim | Sim |
| `MANAGER` | Projetos atribuídos | Sim | Sim | Não |
| `TESTER` | Projetos atribuídos | Sim | Sim | Não |
| `VIEWER` | Projetos atribuídos | Não | Não | Não |

Os dados de Test Cases, Test Runs e Results são sempre consultados dentro do projeto ativo. A API valida novamente o vínculo do usuário com o projeto; ocultar botões no frontend não é a única proteção.

## Segurança

- Usuários e vínculos com projetos são persistidos no PostgreSQL.
- Senhas são armazenadas com BCrypt.
- Após cinco tentativas incorretas, a conta fica bloqueada por 15 minutos.
- A autenticação usa sessão HTTP com cookie `HttpOnly`.
- Operações de escrita exigem token CSRF.
- CORS aceita apenas as origens configuradas em `CORS_ALLOWED_ORIGINS`.
- A sessão expira após 8 horas de inatividade.
- O Swagger e o contrato OpenAPI exigem autenticação de administrador.

## Validação

```bash
cd backend/backend && ./mvnw test
cd frontend && npm run lint && npm run build
```
