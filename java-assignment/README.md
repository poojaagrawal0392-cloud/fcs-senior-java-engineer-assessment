# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Use JDK 17 or newer and Maven 3.8+. Run Maven from this directory:

```sh
mvn package
```

On Windows, use `.\mvnw.cmd package` if Maven is not installed.

## Running tests

Tests require PostgreSQL. Quarkus Dev Services can start a test database when Docker is
available; otherwise configure `QUARKUS_DATASOURCE_DB_KIND`, `QUARKUS_DATASOURCE_USERNAME`,
`QUARKUS_DATASOURCE_PASSWORD`, and `QUARKUS_DATASOURCE_JDBC_URL` to point at a PostgreSQL
instance. Execute:

```sh
mvn verify
```

The JaCoCo verification step fails the build if line coverage for the application is below
80%. GitHub Actions runs the same verification against PostgreSQL on each push and pull request.

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports live coding:

```sh
mvn quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
mvn package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...

## Fulfillment assignments

The bonus API creates a relationship between an existing store, product, and active warehouse:

```http
POST /fulfillments
Content-Type: application/json

{
  "storeId": 1,
  "productId": 1,
  "warehouseBusinessUnitCode": "MWH.001"
}
```

The API rejects invalid references and enforces no more than two warehouses per product/store,
three warehouses per store, and five distinct products per warehouse.

Warehouse replacement archives the active record and creates a new active record with the same
business unit code. Archived records remain in the database for history but are omitted from
active warehouse listings. Development and test startup recreate the local schema and load the
sample data; production startup updates the schema without dropping existing warehouse history.


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".