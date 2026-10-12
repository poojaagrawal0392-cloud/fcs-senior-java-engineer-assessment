package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseEndpointTest {

  @Test
  void createGetReplaceAndArchiveWarehouse() {
    String code = "MWH.TEST-" + UUID.randomUUID();
    String path = "warehouse";

    given()
        .contentType("application/json")
        .body(warehouse(code, "VETSBY-001", 80, 10))
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .body(containsString(code))
        .body("id", notNullValue());

    given().when().get(path).then().statusCode(200).body(containsString(code));
    given().when().get(path + "/1").then().statusCode(200);
    given().when().get(path + "/" + code).then().statusCode(200);

    given()
        .contentType("application/json")
        .body(warehouse(code, "VETSBY-001", 50, 10))
        .when()
        .post(path + "/" + code + "/replacement")
        .then()
        .statusCode(200)
        .body(containsString(code));

    given().when().delete(path + "/" + code).then().statusCode(204);
    given().when().get(path + "/" + code).then().statusCode(404);
    given().when().delete(path + "/MWH.UNKNOWN").then().statusCode(404);
  }

  @Test
  void rejectsInvalidCreationAndReplacementRequests() {
    given()
        .contentType("application/json")
        .body(warehouse("MWH.INVALID", "UNKNOWN", 10, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);

    given()
        .contentType("application/json")
        .body(warehouse("MWH.NOT-FOUND", "VETSBY-001", 10, 1))
        .when()
        .post("warehouse/MWH.NOT-FOUND/replacement")
        .then()
        .statusCode(404);

    given()
        .contentType("application/json")
        .body(warehouse("MWH.DIFFERENT", "VETSBY-001", 10, 1))
        .when()
        .post("warehouse/MWH.PATH/replacement")
        .then()
        .statusCode(400);
  }

  private String warehouse(String code, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\"" + code + "\",\"location\":\"" + location
        + "\",\"capacity\":" + capacity + ",\"stock\":" + stock + "}";
  }
}
