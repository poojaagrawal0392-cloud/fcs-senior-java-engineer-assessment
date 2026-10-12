# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
I would converge the warehouse persistence code on one clear repository contract and keep
business rules in use cases rather than mixing them into REST handlers or ORM entities. The
warehouse adapter already demonstrates a port/repository boundary, while Store uses Panache
active-record methods and Product uses a Panache repository. The active-record approach is
concise for small CRUD resources; the repository/port approach is easier to unit-test and to
replace when the domain has multi-step rules such as warehouse replacement. I would use
repositories for the more involved workflows and keep simple CRUD operations consistent with
that choice as the application grows.

I would not introduce a large generic DAO layer just to remove a few lines of Panache code.
The refactor should be incremental: first add repository methods for queries that represent
business concepts (for example, finding the active warehouse for a business-unit code), then
move transaction boundaries around use cases where several writes must succeed or fail
together. I would also use database constraints as a final guard for uniqueness and foreign
keys, while retaining readable application validation for useful client errors.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
An OpenAPI-first approach gives consumers a reviewable contract before implementation, enables
client/server code generation, and helps keep paths, payloads, and status codes consistent. It
is especially valuable for a public or independently deployed API. The trade-offs are generator
configuration and generated-code churn, and a specification can drift from behavior if the
implementation does not honor it.

Handwritten endpoints are quick to build and make framework-specific behavior easy to express.
They avoid generated layers for a small internal API, but schemas and status codes can become
inconsistent and consumers have no canonical contract. I would choose OpenAPI-first for the
warehouse API and gradually give Store and Product equivalent documented contracts, generated
or hand-authored depending on client needs. In either case, CI should validate the contract
against endpoint tests, and generated sources should remain build artifacts rather than being
manually edited.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would begin with deterministic unit tests for domain rules: unknown locations, duplicate
business-unit codes, location warehouse-count and capacity limits, stock validation, replacement
matching, and archival history. These tests are fast and isolate the highest-risk rules. Then I
would add focused persistence/API integration tests for status codes, JSON mapping, database
constraints, transaction boundaries, and the Store-to-legacy commit ordering. A small number of
end-to-end tests should verify the most important user journeys rather than duplicate every
unit case.

Coverage is a signal, not a substitute for assertions. I would measure line coverage with
JaCoCo, enforce an 80% minimum in CI for application code, and review uncovered business-rule
branches when code changes. Tests should run on every pull request against the same PostgreSQL
major version used in deployment. For database-backed tests, isolate or clean up test data and
include rollback/error scenarios. Coverage reports and failing tests should be visible in CI so
the team can keep the threshold meaningful rather than adding tests that only execute lines.
```