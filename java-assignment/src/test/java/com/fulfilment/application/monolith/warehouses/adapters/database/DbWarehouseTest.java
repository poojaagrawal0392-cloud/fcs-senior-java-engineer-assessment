package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbWarehouseTest {

  @Test
  void convertsAllPersistenceFieldsToTheDomainModel() {
    DbWarehouse entity = new DbWarehouse();
    entity.id = 42L;
    entity.businessUnitCode = "MWH.042";
    entity.location = "TILBURG-001";
    entity.capacity = 45;
    entity.stock = 17;
    entity.createdAt = LocalDateTime.parse("2025-01-02T03:04:05");
    entity.archivedAt = LocalDateTime.parse("2025-02-03T04:05:06");

    Warehouse result = entity.toWarehouse();

    assertEquals(entity.id, result.id);
    assertEquals(entity.businessUnitCode, result.businessUnitCode);
    assertEquals(entity.location, result.location);
    assertEquals(entity.capacity, result.capacity);
    assertEquals(entity.stock, result.stock);
    assertEquals(entity.createdAt, result.createdAt);
    assertEquals(entity.archivedAt, result.archivedAt);
  }

  @Test
  void preservesNullOptionalPersistenceFields() {
    Warehouse result = new DbWarehouse().toWarehouse();

    assertNull(result.id);
    assertNull(result.createdAt);
    assertNull(result.archivedAt);
  }
}
