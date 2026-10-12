# Warehouse and Fulfillment Management

This Quarkus application is a senior Java engineering assessment focused on warehouse lifecycle
management, store and product APIs, persistence, business validation, and integration boundaries.
It is a small modular monolith: one deployable application, with domain-oriented packages and
separate REST, use-case, and persistence responsibilities where the workflow needs them.

For the original assignment and domain description, see [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md)
and [BRIEFING.md](BRIEFING.md). The assessment's design questions and considered answers are in
[QUESTIONS.md](QUESTIONS.md).

## Contents

- [Business domain](#business-domain)
- [Architecture and components](#architecture-and-components)
- [Data model](#data-model)
- [Warehouse rules and workflows](#warehouse-rules-and-workflows)
- [REST API](#rest-api)
- [Swagger UI and OpenAPI](#swagger-ui-and-openapi)
- [Build, run, and test](#build-run-and-test)
- [Interview preparation](#interview-preparation)
- [Production considerations](#production-considerations)

## Business domain

The application represents four business concepts:

- **Location**: a predefined geographical location with a maximum number of warehouses and a
  maximum combined warehouse capacity.
- **Warehouse**: a storage unit at a location, identified operationally by a business-unit code.
  Warehouses can be archived and replaced; archived records are retained for history.
- **Store**: a physical retail location.
- **Product**: an item offered through a store.

The bonus **fulfillment assignment** associates a store and product with an active warehouse.
The application does not model inventory movement or order processing; it covers registration,
validation, and these relationships.

## Architecture and components

The application uses Quarkus 3, Jakarta REST, CDI, Hibernate ORM with Panache, and PostgreSQL.
The warehouse API contract is described in
[`src/main/resources/openapi/warehouse-openapi.yaml`](src/main/resources/openapi/warehouse-openapi.yaml)
and generated at build time.

```text
HTTP client
    |
    +--> WarehouseResourceImpl --> warehouse operation ports/use cases
    |                                  |                 |
    |                                  |                 +--> LocationResolver
    |                                  +--> WarehouseStore
    |                                             |
    |                                      WarehouseRepository
    |                                             |
    +-------------------------------------- PostgreSQL
    |
    +--> StoreResource / ProductResource --> Panache entities/repository
    |
    +--> FulfillmentAssignmentResource --> assignment + store/product/warehouse lookups
```

### Main components

| Component | Responsibility |
|---|---|
| `warehouses.adapters.restapi.WarehouseResourceImpl` | Implements the generated warehouse REST contract, maps transport beans to domain models, calls use cases, and maps responses. |
| `warehouses.domain.usecases.CreateWarehouseUseCase` | Validates and creates an active warehouse. |
| `warehouses.domain.usecases.ReplaceWarehouseUseCase` | Validates and performs the archive-and-create replacement workflow. |
| `warehouses.domain.usecases.ArchiveWarehouseUseCase` | Archives an active warehouse while retaining its record. |
| `warehouses.domain.ports.*Operation` | Defines the use-case contracts injected into the REST adapter. |
| `warehouses.domain.ports.WarehouseStore` | Persistence port consumed by warehouse use cases. |
| `warehouses.domain.ports.LocationResolver` | Location lookup port consumed by warehouse use cases. |
| `warehouses.adapters.database.WarehouseRepository` | Panache persistence adapter implementing `WarehouseStore`; queries active records and maps JPA entities to domain models. |
| `warehouses.adapters.database.DbWarehouse` | JPA entity mapped to the `warehouse` table. |
| `location.LocationGateway` | In-memory catalog of known locations and their limits. |
| `stores.StoreResource` | Store REST operations and the after-commit legacy-system notification. |
| `products.ProductResource` / `ProductRepository` | Product REST and persistence operations. |
| `fulfillment.FulfillmentAssignmentResource` | Validates and creates store/product/warehouse assignments. |
| `WarehouseOperationExceptionMapper` | Converts warehouse domain exceptions to HTTP responses. |
| `WarehouseCreateResponseFilter` | Converts a successful `POST /warehouse` response to `201 Created`. |

The warehouse feature uses ports and adapters to isolate its business rules from the REST and
database implementations. Store uses Panache active-record operations, and Product uses a
Panache repository. This difference is intentional in the assessment and is a useful refactoring
discussion; see [Questions about design choices](QUESTIONS.md).

## Data model

### Warehouse

| Field | Meaning |
|---|---|
| `id` | Generated database primary key; the API exposes it as a string. |
| `businessUnitCode` | Business identifier. Only one active warehouse should use a code; a replacement reuses it after archiving the prior record. |
| `location` | Identifier resolved against the location catalog. |
| `capacity` | Maximum stock capacity for this warehouse. |
| `stock` | Current stock. Must not be negative or exceed capacity. |
| `createdAt` | Creation timestamp. |
| `archivedAt` | Archive timestamp; `null` represents an active record. |

### Location

The in-memory catalog provides `identification`, `maxNumberOfWarehouses`, and `maxCapacity`.
The latter two constraints apply to active warehouses at that location.

### Fulfillment assignment

An assignment records `storeId`, `productId`, and `warehouseBusinessUnitCode`. A unique
constraint prevents duplicate triples. The referenced store, product, and active warehouse are
validated before the assignment is stored.

## Warehouse rules and workflows

### Create

1. The REST adapter maps the request to a domain `Warehouse`.
2. The create use case requires a non-blank business-unit code and rejects a duplicate active
   code.
3. It requires positive capacity, non-negative stock, and `stock <= capacity`.
4. It resolves the location and rejects unknown locations.
5. It checks both the location's active-warehouse count and the sum of active warehouse
   capacities against location limits.
6. It sets `createdAt`, clears `archivedAt`, and persists the warehouse.

### Replace

1. The path business-unit code identifies the current active record. If a code is also supplied
   in the body, it must match the path.
2. The current active warehouse must exist.
3. Replacement stock must equal the current warehouse's stock, and new capacity must hold it.
4. The new location and its warehouse-count/capacity limits are validated, excluding the
   warehouse being replaced from active-location calculations.
5. The existing record is archived and a new active record is created with the same
   business-unit code.

The endpoint is transactional, so the archive and new record should succeed or fail as one
database operation. Retaining the archived row preserves warehouse history.

### Archive

An archive request resolves the active warehouse by database ID or business-unit code. The
record receives an `archivedAt` timestamp; repeated archival is rejected. Active list and
business-unit queries exclude archived records.

### Fulfillment constraints

`POST /fulfillments` enforces:

- At most two distinct warehouses for a given store/product pair.
- At most three distinct warehouses for a store.
- At most five distinct products for a warehouse.
- No duplicate store/product/warehouse assignment.

### Error handling

Warehouse domain validation uses HTTP `400` for invalid input, `404` for missing active
warehouses, and `409` for conflicts such as duplicate active business-unit codes or repeated
archival. Fulfillment validation uses `400` for malformed requests, `404` for unknown
references, and `409` for violated assignment limits or duplicates.

## REST API

The server listens on port `8080` by default. The generated warehouse contract is the canonical
source for warehouse endpoint paths and request/response schemas.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/warehouse` | List active warehouses. |
| `POST` | `/warehouse` | Create a warehouse; successful responses use `201 Created`. |
| `GET` | `/warehouse/{id}` | Get an active warehouse by numeric ID or business-unit code. |
| `DELETE` | `/warehouse/{id}` | Archive an active warehouse. |
| `POST` | `/warehouse/{businessUnitCode}/replacement` | Replace the active warehouse for a business-unit code. |
| `GET` | `/store` | List stores. |
| `GET` | `/store/{id}` | Get a store. |
| `POST` | `/store` | Create a store. |
| `PUT` | `/store/{id}` | Update a store. |
| `PATCH` | `/store/{id}` | Partially update a store. |
| `DELETE` | `/store/{id}` | Delete a store. |
| `GET` | `/product` | List products. |
| `GET` | `/product/{id}` | Get a product. |
| `POST` | `/product` | Create a product. |
| `PUT` | `/product/{id}` | Update a product. |
| `DELETE` | `/product/{id}` | Delete a product. |
| `POST` | `/fulfillments` | Assign an active warehouse to a store/product pair. |

### Example requests

Create a warehouse:

```http
POST /warehouse
Content-Type: application/json

{
  "businessUnitCode": "MWH.101",
  "location": "ZWOLLE-001",
  "capacity": 30,
  "stock": 10
}
```

Replace a warehouse:

```http
POST /warehouse/MWH.101/replacement
Content-Type: application/json

{
  "businessUnitCode": "MWH.101",
  "location": "ZWOLLE-001",
  "capacity": 35,
  "stock": 10
}
```

Create a fulfillment assignment:

```http
POST /fulfillments
Content-Type: application/json

{
  "storeId": 1,
  "productId": 1,
  "warehouseBusinessUnitCode": "MWH.001"
}
```

## Swagger UI and OpenAPI

The Quarkus SmallRye OpenAPI and Swagger UI extensions are included.

- **Swagger UI (development/test):** `http://localhost:8080/q/swagger-ui/`
- **OpenAPI document:** `http://localhost:8080/q/openapi`

When running on another port, substitute that port in the URLs. Swagger UI is normally
available by default in development and test mode. To include the UI in a production build,
configure `quarkus.swagger-ui.always-include=true` deliberately.

The warehouse API definition is maintained in
`src/main/resources/openapi/warehouse-openapi.yaml`; the generator creates Java API interfaces
and beans during the Maven build. Do not edit generated classes by hand.

## Build, run, and test

### Requirements

- JDK 17 or newer.
- Maven 3.8+ or the included Maven wrapper.
- PostgreSQL for database-backed tests and production-style execution. Docker can be used to
  provide PostgreSQL locally.

Confirm `JAVA_HOME` points to the JDK and that `java -version` reports Java 17 or later.

### Build

From this directory:

```sh
mvn package
```

On Windows, the wrapper can be used if Maven is not installed:

```powershell
.\mvnw.cmd package
```

### Start in development mode

```sh
mvn quarkus:dev
```

On Windows:

```powershell
.\mvnw.cmd quarkus:dev
```

Quarkus dev mode provides live reload. With Docker available, Dev Services can start a
PostgreSQL instance automatically. Without Docker, set
`QUARKUS_DATASOURCE_DB_KIND`, `QUARKUS_DATASOURCE_USERNAME`,
`QUARKUS_DATASOURCE_PASSWORD`, and `QUARKUS_DATASOURCE_JDBC_URL` to a reachable PostgreSQL
database before starting the application.

### Local PostgreSQL with Docker

```sh
docker run --rm --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

The production profile uses `jdbc:postgresql://localhost:15432/quarkus_test` and the matching
`quarkus_test` username. Connection and Hibernate settings are in
`src/main/resources/application.properties`. Development and test profiles recreate the local
schema and load sample data; production updates the schema and disables the sample import.

### Run tests

Tests require PostgreSQL. Quarkus Dev Services can start it when Docker is available; otherwise
configure the datasource environment variables above. Run:

```sh
mvn verify
```

The JaCoCo verification enforces at least 80% line coverage for application code. GitHub
Actions runs verification with PostgreSQL provisioned by the workflow. Coverage is a useful
signal, but assertions should target business rules and observable outcomes rather than simply
executing lines.

### Run the packaged application

```sh
mvn package
java -jar ./target/quarkus-app/quarkus-run.jar
```

Ensure PostgreSQL is available and datasource settings are configured for the selected runtime
profile.

## Interview preparation

### A concise architecture explanation

> This is a Quarkus modular monolith. The warehouse REST adapter implements an OpenAPI-generated
> contract and maps transport beans to domain models. Warehouse use cases own the lifecycle
> rules and depend on ports for persistence and location resolution. A Panache repository
> implements the persistence port over PostgreSQL. Store and Product demonstrate simpler
> Panache patterns, while fulfillment adds cross-entity validation. The design keeps the
> multi-step warehouse rules testable without introducing distributed-system complexity.

### Common interview questions and answer points

1. **Why use cases and ports for the warehouse workflow?**
   They isolate business rules from HTTP and ORM details, make rules independently testable,
   and let the persistence adapter change without changing the use cases. For simple CRUD, the
   additional abstraction may not be worthwhile; apply it where it improves clarity.

2. **Why retain archived warehouse rows?**
   Archive-and-create replacement preserves the history behind a business-unit code. Active
   queries filter out archived records, while audit and historical views can still inspect
   prior versions.

3. **How do you make replacement safe?**
   Validate the active record, stock equality, capacity, location and location limits before
   writing. Keep archive and insert in one transaction. Add database constraints and concurrency
   control where simultaneous requests could otherwise pass the same pre-check.

4. **Why use OpenAPI-first for Warehouse but handwritten Store/Product resources?**
   OpenAPI-first gives consumers an explicit contract and supports code generation, at the cost
   of generator setup and generated-code management. Handwritten endpoints are quick for small
   internal APIs, but need equivalent documentation and contract tests to avoid drift. Keep
   generated files as build artifacts.

5. **How is Store synchronization ordered relative to the database?**
   Store create/update registers a transaction synchronization and calls the legacy gateway only
   after transaction completion reports a commit. For reliable delivery across process failures,
   use a transactional outbox rather than relying solely on an in-process callback.

6. **What tests have the highest value?**
   Unit-test each boundary condition in use cases, then integration-test REST status codes,
   persistence mappings, transaction behavior, and legacy notification ordering. Add a few
   end-to-end flows for create, replace, archive, and fulfillment. Use JaCoCo's 80% line threshold
   as a guardrail, not a replacement for meaningful assertions.

7. **What production improvements would you prioritize?**
   Enforce active business-unit uniqueness and stock/capacity invariants in the database where
   appropriate; handle concurrent capacity reservations atomically; add an outbox for legacy
   events; standardize error responses and validation; add authentication, authorization,
   observability, migration-based schema changes, and API contract tests.

8. **Would you split this monolith into microservices?**
   Not without independent scaling, ownership, or deployment needs. A modular monolith keeps
   transactions and operations simpler. If boundaries later require separation, use explicit
   domain ownership and asynchronous integration with idempotent consumers.

### Suggested test matrix

| Area | Example cases |
|---|---|
| Location lookup | Known identifier; unknown identifier; null identifier. |
| Warehouse creation | Valid input; blank/duplicate code; unknown location; capacity/stock invalid; location count or total capacity reached. |
| Replacement | Existing code; unknown code; mismatched stock; insufficient capacity; invalid location; successful history-preserving replacement. |
| Archive | Active record; unknown identifier; repeated archive; archived record excluded from active list. |
| Store integration | Gateway called only after successful commit; no gateway call after rollback. |
| Fulfillment | Missing references; duplicate assignment; each of the three limits at and beyond its boundary. |
| API contract | HTTP status, error payload, JSON mapping, and OpenAPI schema match expected behavior. |

## Production considerations

- **Uniqueness and concurrency:** application pre-checks improve error messages but are not
  sufficient under concurrent requests. Add suitable unique constraints/indexes and define
  handling for constraint violations. Capacity checks that involve sums need an atomic strategy
  (for example, locking the location aggregate or reserving capacity transactionally).
- **Replacement history:** the current warehouse entity stores archive history by row, while
  active lookup filters by `archivedAt is null`. The business-unit code should be unique among
  active records; a plain global unique constraint on that code would prevent replacement
  history from reusing it.
- **Reliable legacy integration:** an after-commit callback avoids notifying the legacy system
  for rolled-back writes, but a process can still fail after commit and before delivery. A
  transactional outbox with retries and idempotency is a stronger production pattern.
- **Schema lifecycle:** development/test schema recreation is convenient but destructive to
  local data. Production should use reviewed database migrations and backups rather than relying
  on automatic schema updates for long-lived data.
- **API consistency:** Warehouse has an OpenAPI contract; Store and Product are handwritten.
  Document and test those APIs as well, and ensure error formats and status codes are consistent.

## Troubleshooting

- If the build reports a missing Java version, set `JAVA_HOME` to a JDK 17+ installation and
  reopen the shell.
- If datasource startup fails, ensure PostgreSQL is reachable, credentials and JDBC URL match,
  or enable Docker for Quarkus Dev Services.
- If Swagger UI is not available in development, verify the `quarkus-smallrye-openapi` and
  `quarkus-swagger-ui` dependencies are present and restart the application.
- In IntelliJ, if generated API classes are not recognized, mark the generated source directory
  under `target` as generated sources; never manually edit generated files.