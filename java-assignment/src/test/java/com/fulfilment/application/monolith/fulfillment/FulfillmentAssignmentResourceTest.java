package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FulfillmentAssignmentResourceTest {

  @Test
  void enforcesAssignmentLimits() {
    List<String> warehouses = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      warehouses.add(createWarehouse());
    }

    assign(1, 1, warehouses.get(0), 201);
    assign(1, 1, warehouses.get(1), 201);
    assign(1, 1, warehouses.get(2), 409);
    assign(1, 2, warehouses.get(2), 201);
    assign(1, 3, warehouses.get(3), 409);
    assign(1, 1, warehouses.get(0), 409);

    int product4 = createProduct();
    int product5 = createProduct();
    int product6 = createProduct();
    assign(2, 1, warehouses.get(3), 201);
    assign(2, 2, warehouses.get(3), 201);
    assign(2, 3, warehouses.get(3), 201);
    assign(2, product4, warehouses.get(3), 201);
    assign(2, product5, warehouses.get(3), 201);
    assign(2, product6, warehouses.get(3), 409);
  }

  @Test
  void rejectsMissingReferencesAndMalformedRequests() {
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .post("fulfillments")
        .then()
        .statusCode(400);
    assign(999999, 1, "MWH.001", 404);
    assign(1, 999999, "MWH.001", 404);
    assign(1, 1, "MWH.UNKNOWN", 404);
  }

  private String createWarehouse() {
    String code = "MWH.BONUS-" + UUID.randomUUID();
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"" + code
            + "\",\"location\":\"AMSTERDAM-001\",\"capacity\":10,\"stock\":1}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);
    return code;
  }

  private int createProduct() {
    String name = "bp-" + UUID.randomUUID().toString().replace("-", "");
    return given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"stock\":1}")
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private void assign(int storeId, int productId, String warehouseCode, int statusCode) {
    given()
        .contentType("application/json")
        .body("{\"storeId\":" + storeId + ",\"productId\":" + productId
            + ",\"warehouseBusinessUnitCode\":\"" + warehouseCode + "\"}")
        .when()
        .post("fulfillments")
        .then()
        .statusCode(statusCode);
  }
}
