package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StoreResourceTest {

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  void resetGatewayMock() {
    reset(legacyStoreManagerGateway);
  }

  @Test
  void createNotifiesLegacyOnlyAfterTheStoreCanBeReadFromTheDatabase() {
    doAnswer(
            invocation -> {
              Store committed = invocation.getArgument(0);
              assertNotNull(Store.findById(committed.id));
              return null;
            })
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any(Store.class));

    given()
        .contentType("application/json")
        .body("{\"name\":\"" + uniqueName() + "\",\"quantityProductsInStock\":7}")
        .when()
        .post("store")
        .then()
        .statusCode(201);

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  void updateAndPatchNotifyLegacyWithCommittedEntity() {
    doAnswer(
            invocation -> {
              Store committed = invocation.getArgument(0);
              assertNotNull(Store.findById(committed.id));
              return null;
            })
        .when(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(any(Store.class));

    given()
        .contentType("application/json")
        .body("{\"name\":\"store-update-" + UUID.randomUUID()
            + "\",\"quantityProductsInStock\":18}")
        .when()
        .put("store/1")
        .then()
        .statusCode(200);

    given()
        .contentType("application/json")
        .body("{\"name\":\"store-patch-" + UUID.randomUUID()
            + "\",\"quantityProductsInStock\":19}")
        .when()
        .patch("store/1")
        .then()
        .statusCode(200);

    verify(legacyStoreManagerGateway, org.mockito.Mockito.times(2))
        .updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  void supportsStoreReadDeleteAndValidationErrors() {
    given().when().get("store").then().statusCode(200);
    given().when().get("store/1").then().statusCode(200);
    given().when().get("store/999999").then().statusCode(404);
    given()
        .contentType("application/json")
        .body("{\"id\":1,\"name\":\"invalid\"}")
        .when()
        .post("store")
        .then()
        .statusCode(422);
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\":10}")
        .when()
        .put("store/1")
        .then()
        .statusCode(422);
    given()
        .contentType("application/json")
        .body("{\"name\":\"valid\"}")
        .when()
        .put("store/999999")
        .then()
        .statusCode(404);
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\":10}")
        .when()
        .patch("store/1")
        .then()
        .statusCode(422);
    given()
        .contentType("application/json")
        .body("{\"name\":\"not-found\"}")
        .when()
        .patch("store/999999")
        .then()
        .statusCode(404);

    long id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + uniqueName() + "\",\"quantityProductsInStock\":1}")
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    given().when().delete("store/" + id).then().statusCode(204);
    given().when().delete("store/" + id).then().statusCode(404);
  }

  @Test
  void doesNotNotifyLegacySystemWhenDatabaseCommitFails() {
    String duplicateName = uniqueName();
    given()
        .contentType("application/json")
        .body("{\"name\":\"" + duplicateName + "\",\"quantityProductsInStock\":3}")
        .when()
        .post("store")
        .then()
        .statusCode(201);
    reset(legacyStoreManagerGateway);

    given()
        .contentType("application/json")
        .body("{\"name\":\"" + duplicateName + "\",\"quantityProductsInStock\":3}")
        .when()
        .post("store")
        .then()
        .statusCode(500);

    verifyNoInteractions(legacyStoreManagerGateway);
  }

  private String uniqueName() {
    return "s-" + UUID.randomUUID().toString().replace("-", "");
  }
}
