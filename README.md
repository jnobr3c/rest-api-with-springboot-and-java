# REST API com Spring Boot e Java

Documentação técnica do projeto `rest-api-with-springboot-and-java`, especificamente da etapa **09_DTO-DataTransferObject**.

Repositório: https://github.com/jnobr3c/rest-api-with-springboot-and-java

---

## 1. Visão geral

Este projeto é uma API REST simples de CRUD (Create, Read, Update, Delete) de **Pessoas (Person)**, construída com Spring Boot 3.4.0 e Java 21. É um projeto de estudo, provavelmente parte de um curso, em que cada etapa (branch/pasta) introduz um novo conceito. Esta etapa (09) introduz o padrão **DTO (Data Transfer Object)** usando a biblioteca **Dozer Mapper** para conversão automática entre entidade JPA e objeto de transferência.

**Stack utilizada:**

| Tecnologia | Papel |
|---|---|
| Spring Boot 3.4.0 | Framework principal |
| Spring Web (MVC) | Criação da API REST |
| Spring Data JPA | Persistência/ORM |
| MySQL Connector/J | Driver de banco de dados |
| Dozer Mapper 7.0.0 | Mapeamento automático Entity ↔ DTO |
| Java 21 | Linguagem |
| JUnit 5 | Testes |

---

## 2. Estrutura de pacotes

```
br.com.nobre
├── Startup.java                     # classe main (bootstrap do Spring Boot)
├── controllers/
│   ├── PersonController.java        # endpoints REST de Person
│   └── TestLogController.java       # endpoint de teste de logs
├── data/dto/
│   └── PersonDTO.java               # objeto de transferência de dados
├── model/
│   └── Person.java                  # entidade JPA (tabela "person")
├── repository/
│   └── PersonRepository.java        # interface JPA Repository
├── services/
│   └── PersonServices.java          # regras de negócio
├── mapper/
│   └── ObjectMapper.java            # wrapper estático do Dozer
└── exception/
    ├── ResourceNotFoundException.java
    ├── ExceptionResponse.java
    └── handler/CustomEntityResponseHandler.java
```

Essa organização segue o padrão em camadas clássico do Spring:

```
Controller → Service → Repository → Banco de Dados
     ↑            ↑
   DTO         Entity (com mapper convertendo entre os dois)
```

---

## 3. Por que usar DTO?

O **DTO (Data Transfer Object)** é um objeto simples usado para transportar dados entre camadas — no caso, entre a API e o cliente — **sem expor a entidade JPA diretamente**.

Vantagens que esse padrão traz ao projeto:

- **Desacoplamento**: mudanças na entidade `Person` (ex.: nova coluna sensível no banco) não vazam automaticamente para o contrato JSON exposto pela API.
- **Segurança**: evita expor detalhes internos de persistência (anotações JPA, relacionamentos lazy, etc.) no payload HTTP.
- **Serialização controlada**: o DTO define exatamente o que trafega na API, independente do modelo de dados.

Neste projeto:
- `Person` (em `model/`) é a **entidade JPA**, anotada com `@Entity`, `@Table`, `@Id`, `@Column` — representa a tabela `person` no MySQL.
- `PersonDTO` (em `data/dto/`) é uma **classe espelho**, sem nenhuma anotação JPA, com os mesmos campos (`id`, `firstName`, `lastName`, `address`, `gender`).

A conversão entre os dois é feita automaticamente pelo **Dozer Mapper**, evitando escrever manualmente dezenas de `getters/setters` de conversão.

---

## 4. O `ObjectMapper` (wrapper do Dozer)

```java
public class ObjectMapper {
    private static Mapper mapper = DozerBeanMapperBuilder.buildDefault();

    public static <O, D> D parseObject(O origin, Class<D> destination) {
        return mapper.map(origin, destination);
    }

    public static <O, D> List<D> parseListObjects(List<O> origin, Class<D> destination) {
        List<D> destinationsObjects = new ArrayList<>();
        for (Object o : origin) {
            destinationsObjects.add(mapper.map(o, destination));
        }
        return destinationsObjects;
    }
}
```

- `parseObject`: converte um único objeto de um tipo para outro (ex.: `Person` → `PersonDTO`), comparando campos com o mesmo nome via reflexão.
- `parseListObjects`: mesma coisa, mas para listas — usado, por exemplo, ao converter `List<Person>` retornada pelo repositório em `List<PersonDTO>` para a API.

Esse mapeamento é genérico (usa generics `<O, D>`), então serve tanto para `Entity → DTO` quanto para `DTO → Entity`.

---

## 5. Camada de serviço (`PersonServices`)

Responsável pela lógica de negócio. Sempre trabalha com `PersonDTO` na entrada/saída pública, mas converte para `Person` internamente antes de falar com o banco:

| Método | O que faz |
|---|---|
| `findAll()` | Busca todas as pessoas no banco e converte a lista `Person` → `PersonDTO` |
| `findById(Long id)` | Busca uma pessoa por ID; se não existir, lança `ResourceNotFoundException` |
| `create(PersonDTO person)` | Converte DTO → Entity, salva no repositório, converte o resultado salvo de volta para DTO |
| `update(PersonDTO person)` | Busca a entidade existente pelo ID do DTO, atualiza campo a campo, salva e retorna DTO |
| `delete(Long id)` | Busca a entidade e remove do banco |

Todos os métodos logam a ação usando SLF4J (`logger.info(...)`), útil para observabilidade.

---

## 6. Camada de controller (`PersonController`)

Expõe os endpoints REST, todos sob o prefixo `/person`:

| Verbo HTTP | Endpoint | Descrição | Corpo (Request) | Resposta |
|---|---|---|---|---|
| `GET` | `/person` | Lista todas as pessoas | — | `List<PersonDTO>` (JSON) |
| `GET` | `/person/{id}` | Busca uma pessoa por ID | — | `PersonDTO` (JSON) |
| `POST` | `/person` | Cria uma nova pessoa | `PersonDTO` (JSON) | `PersonDTO` criado |
| `PUT` | `/person` | Atualiza uma pessoa existente (ID vem no corpo) | `PersonDTO` (JSON) | `PersonDTO` atualizado |
| `DELETE` | `/person/{id}` | Remove uma pessoa por ID | — | `204`/vazio |

Exemplo de payload JSON aceito/retornado:

```json
{
  "id": 1,
  "firstName": "Leandro",
  "lastName": "Costa",
  "address": "Rua das Flores, 123",
  "gender": "Male"
}
```

Observação de design: o `PersonController` usa `@RequestMapping` genérico com `method = RequestMethod.X` em vez das anotações mais modernas (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`). Funciona igual, mas é um estilo mais verboso — comum em projetos de curso para deixar explícito o funcionamento por trás das anotações de atalho.

Há também um controller à parte, `TestLogController`, com um único endpoint `GET /test`, que apenas gera logs em todos os níveis (`debug`, `info`, `warn`, `error`) — serve para testar a configuração de logging da aplicação, não faz parte do domínio de negócio.

---

## 7. Tratamento de exceções

O projeto usa `@ControllerAdvice` para centralizar o tratamento de erros (`CustomEntityResponseHandler`):

- **`ResourceNotFoundException`** → capturada especificamente, retorna HTTP **404 Not Found** com um corpo padronizado.
- **`Exception` genérica** → capturada como fallback, retorna HTTP **500 Internal Server Error**.

O corpo de erro segue o formato do record `ExceptionResponse`:

```json
{
  "timestamp": "2026-07-02T10:00:00.000+00:00",
  "message": "No records found with ID: 99",
  "details": "uri=/person/99"
}
```

Pequeno detalhe de atenção: a classe `ResourceNotFoundException` está anotada com `@ResponseStatus(HttpStatus.BAD_REQUEST)` (400), mas o handler no `CustomEntityResponseHandler` devolve `HttpStatus.NOT_FOUND` (404) para essa mesma exceção. Como o `@ExceptionHandler` tem prioridade sobre o `@ResponseStatus` da classe, o comportamento real em runtime é **404**, mesmo a anotação da exceção dizendo 400 — vale ajustar isso para não confundir quem for ler o código.

---

## 8. Persistência (`Person` + `PersonRepository`)

- `Person` é uma entidade JPA simples mapeada para a tabela `person`, com os campos `id` (chave primária auto-incremento), `first_name`, `last_name`, `address` e `gender`.
- `PersonRepository extends JpaRepository<Person, Long>` — sem métodos customizados, aproveita tudo que o Spring Data JPA já fornece de fábrica (`findAll`, `findById`, `save`, `delete`, etc.).

## 9. Configuração (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rest_api_with_springboot?useTimezone=true&serverTimeZone=UTC
    username: root
    password: admin123
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

- `ddl-auto: update` faz o Hibernate criar/atualizar as tabelas automaticamente com base nas entidades — conveniente para desenvolvimento, mas não recomendado em produção.
- **Atenção**: há um pequeno erro de sintaxe YAML no arquivo original — `name:rest-api-with-springboot-and-java` está sem espaço depois dos dois-pontos (deveria ser `name: rest-api-with-springboot-and-java`). Isso pode causar erro de parsing do YAML dependendo da versão do Spring Boot/SnakeYAML. Vale corrigir.
- Também é preciso ter um banco MySQL local rodando na porta `3306` com um schema chamado `rest_api_with_springboot` e credenciais `root`/`admin123` (ou ajustar esses valores para o seu ambiente).

## 10. Testes

- `StartupTests`: teste de contexto padrão do Spring Boot (`contextLoads`), garante que a aplicação sobe sem erros de configuração.
- `ObjectMapperTests`: testes unitários do mapeamento Dozer, usando a classe utilitária `MockPerson` para gerar objetos `Person`/`PersonDTO` de teste. Cobre:
    - conversão de uma `Person` para `PersonDTO`;
    - conversão de uma lista de `Person` para lista de `PersonDTO`;
    - conversão de um `PersonDTO` para `Person`;
    - conversão de uma lista de `PersonDTO` para lista de `Person`.

Esses testes validam que o Dozer está de fato copiando os campos corretamente entre as duas classes espelhadas.

---

## 11. Como rodar o projeto

Pré-requisitos: Java 21, Maven, MySQL rodando localmente.

```bash
# 1. Criar o schema no MySQL (se ainda não existir)
mysql -u root -p -e "CREATE DATABASE rest_api_with_springboot;"

# 2. Ajustar usuário/senha em src/main/resources/application.yml se necessário

# 3. Rodar a aplicação
./mvnw spring-boot:run
```

A aplicação sobe por padrão na porta `8080`. Endpoints disponíveis em `http://localhost:8080/person`.

Exemplo de teste rápido com `curl`:

```bash
curl -X POST http://localhost:8080/person \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Leandro","lastName":"Costa","address":"Rua A, 123","gender":"Male"}'
```

---

## 12. Resumo do fluxo de uma requisição

```
Cliente HTTP
   │  JSON (PersonDTO)
   ▼
PersonController          (recebe/devolve DTO)
   │
   ▼
PersonServices             (orquestra a regra de negócio)
   │  DTO → Entity (via ObjectMapper/Dozer)
   ▼
PersonRepository            (JPA/Hibernate)
   │
   ▼
Banco MySQL (tabela person)
   │  Entity → DTO (via ObjectMapper/Dozer) na volta
   ▼
Cliente HTTP recebe PersonDTO em JSON
```

---

## 13. Pontos de atenção / possíveis melhorias

1. **YAML inválido**: corrigir `name:rest-api-with-springboot-and-java` → `name: rest-api-with-springboot-and-java`.
2. **Status HTTP inconsistente** entre `@ResponseStatus` da exceção (400) e o que o handler realmente retorna (404).
3. **Senha em texto puro** no `application.yml` (`admin123`) — em um projeto real, usar variáveis de ambiente ou um cofre de segredos.
4. **Sem `@Valid`/Bean Validation** nos DTOs — não há validação de campos obrigatórios nas requisições `POST`/`PUT`.
5. **`update` requer o ID dentro do corpo** em vez de vir na URL (`PUT /person/{id}`), o que foge um pouco da convenção REST mais comum.

---

## 14. Histórico e evolução do repositório (todas as branches)

O repositório **não usa uma única branch com todo o histórico**: cada etapa do curso vive em uma branch numerada própria, e a branch `main` é, na verdade, **uma trilha separada** (um mini-desafio de operações matemáticas — `MathController`, `SimpleMath`, `GreetingController`), não relacionada ao CRUD de Person. Ou seja, `main` **não** é a "versão final" do CRUD de pessoas — as branches numeradas nunca foram mescladas de volta a ela.

Branches encontradas (via `git ls-remote`):

| Branch | Conteúdo |
|---|---|
| `main` | Projeto inicial "Math/Greeting" (operações aritméticas via API) — trilha separada de aprendizado |
| `06_WorkingWithVerbs` | Primeira versão do CRUD de Person, **em memória** (sem banco), com verbos HTTP completos |
| `07_Spring-Data-JPA_and_Integration_API_with_MySQL` | Introdução de JPA + MySQL real via `PersonRepository` |
| `08_Trabalhando_Com_Logs_no_SpringBoot` | Configuração de níveis de log por pacote |
| `09_DTO-DataTransferObject` | Introdução do padrão DTO com Dozer Mapper (a branch analisada em detalhe acima) |

### 14.1 Branch `06_WorkingWithVerbs` — CRUD em memória

- `PersonServices` fica no pacote raiz (`br.com.nobre.PersonServices`, ainda não em `services/`) e **não usa banco de dados**: os dados são gerados via `mockPerson()`, um contador `AtomicLong` e listas em memória — nada é persistido de fato.
- `Person` ainda tem o campo com erro de digitação **`adress`** (sem o segundo "d"), corrigido nas etapas seguintes para `address`.
- Não existe `PersonRepository` nem `ResourceNotFoundException` ainda — o tratamento de erro herdado é o `UnsupportedMathOperationException`, resquício da trilha "Math" do `main`.
- Já implementa todos os verbos HTTP (`GET`, `POST`, `PUT`, `DELETE`) no `PersonController`, só que sem persistência real por trás.

### 14.2 Branch `07_Spring-Data-JPA_and_Integration_API_with_MySQL` — chegada do banco de dados

Principais mudanças em relação à `06`:

- Criado `PersonRepository extends JpaRepository<Person, Long>` — primeira vez que o projeto fala com um banco de verdade.
- `PersonServices` migra para o pacote `services/` e passa a injetar `PersonRepository` via `@Autowired`, usando `findAll()`, `findById()`, `save()` e `delete()` reais.
- `Person` vira uma entidade JPA (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`) e o campo `adress` é corrigido para `address`.
- Adicionada `ResourceNotFoundException`, lançada quando um `findById` não encontra registro — substituindo o resquício `UnsupportedMathOperationException` herdado do `main`.
- `application.yml` ganha a seção `datasource` apontando para MySQL local.

### 14.3 Branch `08_Trabalhando_Com_Logs_no_SpringBoot` — configuração de logs

Mudança pequena e focada, em relação à `07`:

- Criado `TestLogController`, com o endpoint `GET /test` só para gerar uma linha de log em cada nível (`debug`, `info`, `warn`, `error`) e validar a configuração.
- `application.yml` ganha a seção:
  ```yaml
  logging:
    level:
      #root: WARN
      br.com.nobre: DEBUG
  ```
  configurando o nível `DEBUG` só para o pacote `br.com.nobre`, deixando o restante (Spring, Hibernate etc.) no nível padrão.
- `PersonServices` recebe pequenos ajustes de log (troca de `java.util.logging.Logger` por SLF4J, mais alinhado ao ecossistema Spring).

### 14.4 Branch `09_DTO-DataTransferObject` — a etapa que você enviou

Em relação à `08`, esta é a branch mais "pesada" em termos de mudança de arquitetura (8 arquivos alterados, 281 linhas adicionadas):

- Adiciona a dependência `dozer-core` no `pom.xml`.
- Cria `PersonDTO` (classe nova, 68 linhas) espelhando `Person`, mas sem anotações JPA.
- Cria o utilitário `ObjectMapper` (26 linhas) encapsulando o Dozer (`parseObject`/`parseListObjects`).
- `PersonServices` passa a converter `Entity ↔ DTO` em todos os métodos (`findAll`, `findById`, `create`, `update`, `delete`).
- `PersonController` passa a receber/retornar `PersonDTO` em vez de `Person` diretamente.
- Adiciona testes novos: `ObjectMapperTests` e a classe de apoio `MockPerson`, validando a conversão em ambas as direções (Entity→DTO e DTO→Entity), inclusive para listas.

### 14.5 Linha do tempo resumida

```
main (trilha separada: Math/Greeting API)
      │
      ▼ (não há merge — é uma ramificação independente)
06_WorkingWithVerbs
   CRUD completo, mas 100% em memória (mock)
      │
      ▼
07_Spring-Data-JPA...MySQL
   + PersonRepository real, Person vira @Entity, ResourceNotFoundException
      │
      ▼
08_Trabalhando_Com_Logs
   + TestLogController, configuração de log por pacote
      │
      ▼
09_DTO-DataTransferObject   ← branch que você enviou
   + PersonDTO, ObjectMapper (Dozer), Controller/Service passam a falar em DTO
```

Cada branch, portanto, representa um "checkpoint" pedagógico: o aluno evolui de um mock em memória até um CRUD real com banco, logging estruturado e, por fim, desacoplamento do modelo de persistência via DTO — um padrão comumente ensinado logo antes de tópicos como validação (Bean Validation), paginação e HATEOAS, que normalmente vêm nas etapas seguintes desse tipo de curso (não presentes neste repositório até o momento).
