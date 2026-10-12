package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    String testProductName = "p-" + shortId();
    int testProductId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + testProductName + "\",\"stock\":1}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete(path + "/" + testProductId).then().statusCode(204);

    // The temporary product should be gone, while the seeded products remain.
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString(testProductName)),
            containsString("TONSTAD"),
            containsString("KALLAX"),
            containsString("BESTÅ"));
  }

  @Test
  public void createsReadsUpdatesAndDeletesProduct() {
    String name = "p-" + shortId().substring(0, 20);
    int id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"description\":\"test\",\"price\":12.50,\"stock\":4}")
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().get("product/" + id).then().statusCode(200).body(containsString(name));
    given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "-updated\",\"description\":\"updated\",\"price\":13.00,\"stock\":5}")
        .when()
        .put("product/" + id)
        .then()
        .statusCode(200)
        .body(containsString(name + "-updated"));
    given().when().delete("product/" + id).then().statusCode(204);
    given().when().get("product/" + id).then().statusCode(404);
  }

  @Test
  public void returnsClientErrorsForInvalidProductRequests() {
    given()
        .contentType("application/json")
        .body("{\"id\":10,\"name\":\"invalid\"}")
        .when()
        .post("product")
        .then()
        .statusCode(422);
    given().when().get("product/999999").then().statusCode(404);
    given()
        .contentType("application/json")
        .body("{\"description\":\"missing name\"}")
        .when()
        .put("product/1")
        .then()
        .statusCode(422);
    given()
        .contentType("application/json")
        .body("{\"name\":\"missing\"}")
        .when()
        .put("product/999999")
        .then()
        .statusCode(404);
    given().when().delete("product/999999").then().statusCode(404);
  }

  private String shortId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
