# Food Delivery Backend

API REST de demonstração para um sistema de delivery, construída com Java 26, Spring Boot, Spring Security, JPA e SQLite.

## Tecnologias

- Java 26
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- JWT com Nimbus JOSE + JWT e assinatura RSA/RS256
- BCrypt para armazenamento de senhas
- Jakarta Validation
- SQLite
- Lombok
- OpenAPI 3 e Swagger UI com Springdoc 3.0.3
- Maven Wrapper

## Requisitos

- JDK 26 instalado e disponível no `PATH`
- Não é necessário instalar Maven: o projeto inclui o Maven Wrapper
- Não é necessário instalar ou configurar um servidor de banco de dados: o SQLite utiliza um arquivo local

Confirme a versão do Java:

```bash
java --version
```

O primeiro número exibido deve ser `26`.

## Executando a aplicação

Na raiz do projeto, execute:

```bash
./mvnw spring-boot:run
```

No Windows, use:

```powershell
.\mvnw.cmd spring-boot:run
```

A API ficará disponível em:

```text
http://localhost:8080
```

O arquivo `database.db` será criado automaticamente na raiz do projeto. As roles, os status dos pedidos e o administrador padrão são inseridos de forma idempotente ao iniciar a aplicação.

## Swagger e documentação OpenAPI

Com a aplicação em execução, abra:

```text
http://localhost:8080/swagger-ui.html
```

A especificação OpenAPI em JSON também está disponível em:

```text
http://localhost:8080/v3/api-docs
```

O Swagger mostra os endpoints, schemas, validações e exemplos de payload. Para testar uma rota protegida pela interface:

1. Execute `POST /api/auth/login`.
2. Copie somente o valor de `accessToken` retornado.
3. Clique em **Authorize** no topo do Swagger.
4. Cole o token no campo `bearerAuth` e confirme.

Não é necessário adicionar o prefixo `Bearer` no campo do Swagger.

## Usuário administrador de demonstração

```text
Email: admin@admin.com
Senha: 1234
Role:  Admin
```

A senha também é armazenada como hash BCrypt. Essas credenciais existem somente para facilitar a avaliação do projeto e devem ser removidas ou substituídas em um ambiente real.

As rotas de criação, edição e remoção de itens e a atualização do status dos pedidos são exclusivas dessa role. O claim `roles` do JWT desse usuário contém `ADMIN`.

## Rotas

### Cadastro

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Cliente Teste",
  "address": "Rua das Flores, 123",
  "email": "cliente@example.com",
  "password": "senha-segura"
}
```

O nome, o endereço, o e-mail e a senha são obrigatórios. Todo cadastro feito por essa rota recebe a role `User`.

O cadastro já autentica o novo usuário, grava o cookie `access_token` e retorna:

```json
{
  "user": {
    "id": 2,
    "name": "Cliente Teste",
    "email": "cliente@example.com",
    "address": "Rua das Flores, 123",
    "role": "User"
  },
  "token": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

Não é necessário chamar a rota de login depois do cadastro.

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "cliente@example.com",
  "password": "senha-segura"
}
```

A resposta contém um `accessToken` com validade de uma hora:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

O mesmo token também é enviado no cookie `access_token` com os atributos `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api` e `Max-Age=3600`. O corpo com o token foi mantido para compatibilidade com clientes que utilizam o header `Authorization`, como o Insomnia.

### Logout

```http
POST /api/auth/logout
```

O logout remove o cookie do navegador. Como o JWT é stateless, uma cópia previamente obtida do token continua válida até sua expiração.

### Listar itens

```http
GET /api/items
Authorization: Bearer <accessToken>
```

No navegador, o header pode ser omitido quando o cookie é enviado com a requisição.

### Criar item — somente Admin

```http
POST /api/items
Authorization: Bearer <accessTokenAdmin>
Content-Type: application/json
```

```json
{
  "name": "Hambúrguer",
  "price": 29.90,
  "stock": 20
}
```

### Editar item — somente Admin

```http
PUT /api/items/1
Authorization: Bearer <accessTokenAdmin>
Content-Type: application/json
```

O corpo possui o mesmo formato da criação e representa todos os dados editáveis do item.

### Remover item — somente Admin

```http
DELETE /api/items/1
Authorization: Bearer <accessTokenAdmin>
```

Um item que já pertença a um pedido não pode ser removido; nesse caso, a API responde com `409 Conflict` para preservar o histórico.

### Criar pedido

```http
POST /api/orders
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "items": [
    { "id": 1, "quantity": 2 },
    { "id": 3, "quantity": 1 }
  ],
  "observations": "Entregar na portaria"
}
```

O pedido é associado ao usuário do JWT e começa com o status `Aguardando confirmação`. A API consulta os preços atuais, calcula o total e baixa as quantidades do estoque dentro da mesma transação. Itens repetidos, quantidades inválidas, itens inexistentes e estoque insuficiente são rejeitados.

### Listar status de pedido

```http
GET /api/order-statuses
Authorization: Bearer <accessToken>
```

A resposta contém os nove status cadastrados na seed, com seus respectivos IDs para uso na atualização administrativa.

### Atualizar status — somente Admin

```http
PATCH /api/orders/1/status
Authorization: Bearer <accessTokenAdmin>
Content-Type: application/json
```

```json
{
  "statusId": 2
}
```

### Listar os pedidos do usuário autenticado

```http
GET /api/orders
Authorization: Bearer <accessToken>
```

Essa rota utiliza o identificador presente no JWT e não retorna pedidos pertencentes a outros usuários.

Cada item do pedido informa sua quantidade. As observações opcionais pertencem ao pedido como um todo:

```json
{
  "id": 1,
  "status": "Aguardando confirmação",
  "total": 59.80,
  "observations": "Entregar na portaria",
  "items": [
    {
      "id": 1,
      "name": "Hambúrguer",
      "price": 29.90,
      "quantity": 2
    }
  ]
}
```

### Listar todos os pedidos — somente Admin

```http
GET /api/admin/orders
Authorization: Bearer <accessTokenAdmin>
```

Os pedidos são retornados do mais recente para o mais antigo e incluem os dados do usuário responsável:

```json
[
  {
    "id": 1,
    "user": {
      "id": 2,
      "name": "Cliente Teste",
      "email": "cliente@example.com",
      "address": "Rua das Flores, 123",
      "role": "User"
    },
    "status": "Aguardando confirmação",
    "total": 59.80,
    "observations": "Entregar na portaria",
    "items": [
      {
        "id": 1,
        "name": "Hambúrguer",
        "price": 29.90,
        "quantity": 2
      }
    ]
  }
]
```

Depois de identificar o pedido, o administrador pode atualizar seu status com `PATCH /api/orders/{orderId}/status`.

## Testando pelo Insomnia

1. Inicie a aplicação.
2. Faça `POST /api/auth/login` usando o administrador padrão ou um usuário cadastrado.
3. Copie o campo `accessToken` da resposta.
4. Nas requisições protegidas, selecione autenticação do tipo **Bearer Token** e cole o token.

## Consumindo pelo front-end

O navegador somente envia cookies em requisições para outra origem quando `credentials` está habilitado:

```javascript
await fetch("http://localhost:8080/api/auth/login", {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    email: "admin@admin.com",
    password: "1234"
  })
});

const orders = await fetch("http://localhost:8080/api/orders", {
  credentials: "include"
});
```

Por padrão, o CORS aceita os front-ends locais nas portas `3000`, `5173` e `4200` e permite credenciais. Em outra origem, configure antes de iniciar:

```bash
CORS_ALLOWED_ORIGINS=https://app.exemplo.com ./mvnw spring-boot:run
```

O cookie é `Secure` por padrão. Se o navegador usado no desenvolvimento não aceitar o cookie em HTTP local, execute somente no ambiente local com:

```bash
JWT_COOKIE_SECURE=false ./mvnw spring-boot:run
```

Em produção, mantenha `JWT_COOKIE_SECURE=true` e HTTPS. `SameSite=Strict` pressupõe que front-end e API estejam no mesmo site; caso estejam em sites diferentes, será necessário avaliar `SameSite=None`, HTTPS e proteção CSRF antes da publicação.

## Executando os testes

```bash
./mvnw test
```

Os testes validam as seeds, o hash BCrypt, o login do administrador, a emissão e validação do JWT, as permissões de Admin, o CRUD de itens, a criação de pedidos com baixa de estoque, a atualização de status e o isolamento dos pedidos por usuário.

## Gerando o pacote executável

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Observações sobre JWT

O par de chaves RSA é gerado em memória a cada inicialização. Consequentemente, tokens emitidos antes de uma reinicialização deixam de ser válidos. Em produção, as chaves devem ser persistidas e carregadas de arquivos PEM, variáveis seguras ou um gerenciador de segredos.
