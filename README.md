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

## Usuário administrador de demonstração

```text
Email: admin@admin.com
Senha: 1234
Role:  Admin
```

A senha também é armazenada como hash BCrypt. Essas credenciais existem somente para facilitar a avaliação do projeto e devem ser removidas ou substituídas em um ambiente real.

Atualmente não há uma rota exclusiva de administrador. A diferença pode ser verificada no claim `roles` do JWT, que será `ADMIN` para esse usuário.

## Rotas

### Cadastro

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "cliente@example.com",
  "password": "senha-segura"
}
```

O nome é opcional. Quando não for informado, será obtido da parte do e-mail anterior ao `@`. Todo cadastro feito por essa rota recebe a role `User`.

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

### Listar itens

```http
GET /api/items
Authorization: Bearer <accessToken>
```

### Listar os pedidos do usuário autenticado

```http
GET /api/orders
Authorization: Bearer <accessToken>
```

Essa rota utiliza o identificador presente no JWT e não retorna pedidos pertencentes a outros usuários.

## Testando pelo Insomnia

1. Inicie a aplicação.
2. Faça `POST /api/auth/login` usando o administrador padrão ou um usuário cadastrado.
3. Copie o campo `accessToken` da resposta.
4. Nas requisições protegidas, selecione autenticação do tipo **Bearer Token** e cole o token.

## Executando os testes

```bash
./mvnw test
```

Os testes validam as seeds, o hash BCrypt, o login do administrador, a emissão e validação do JWT, a proteção das rotas e o isolamento dos pedidos por usuário.

## Gerando o pacote executável

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Observações sobre JWT

O par de chaves RSA é gerado em memória a cada inicialização. Consequentemente, tokens emitidos antes de uma reinicialização deixam de ser válidos. Em produção, as chaves devem ser persistidas e carregadas de arquivos PEM, variáveis seguras ou um gerenciador de segredos.
