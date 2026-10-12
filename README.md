# Senior Java Engineer Coding Assessment

This repository contains a Quarkus warehouse and store management assessment.

## Project files

- [Assignment requirements](java-assignment/CODE_ASSIGNMENT.md)
- [Domain briefing](java-assignment/BRIEFING.md)
- [Case study answers](case-study/CASE_STUDY.md)
- [Technical questions and answers](java-assignment/QUESTIONS.md)
- [Java application setup and API instructions](java-assignment/README.md)

## Build and test

The application requires JDK 17 or newer, Maven 3.8+, and PostgreSQL for integration tests.
From `java-assignment`, run `mvn package` to build or `mvn verify` to run tests and enforce
the 80% line-coverage threshold. GitHub Actions runs the same verification on pushes and pull
requests with PostgreSQL provisioned by the workflow.

## Start the application

Follow the PostgreSQL setup instructions in [the Java application README](java-assignment/README.md),
then run `mvn quarkus:dev` from `java-assignment`.

