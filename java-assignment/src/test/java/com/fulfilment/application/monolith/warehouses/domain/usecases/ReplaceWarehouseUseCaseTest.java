package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private TestWarehouseStore store;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = new TestWarehouseStore();
    store.create(warehouse("MWH.REPLACE", "ZWOLLE-001", 20, 10));
    useCase = new ReplaceWarehouseUseCase(store, new LocationGateway());
  }

  @Test
  public void archivesOldWarehouseAndCreatesReplacementWithSameCode() {
    Warehouse replacement = warehouse("MWH.REPLACE", "ZWOLLE-001", 15, 10);

    useCase.replace(replacement);

    assertEquals(2, store.history.size());
    assertNotNull(store.history.get(0).archivedAt);
    assertEquals("MWH.REPLACE", store.history.get(1).businessUnitCode);
    assertNotNull(replacement.createdAt);
  }

  @Test
  public void rejectsMissingWarehouseStockMismatchAndInsufficientCapacity() {
    assertThrows(WarehouseOperationException.class,
        () -> useCase.replace(warehouse("MWH.MISSING", "ZWOLLE-001", 20, 10)));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.replace(warehouse("MWH.REPLACE", "ZWOLLE-001", 20, 9)));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.replace(warehouse("MWH.REPLACE", "ZWOLLE-001", 9, 10)));
  }

  @Test
  public void validatesNewLocationAndAvailableLocationLimits() {
    assertThrows(WarehouseOperationException.class,
        () -> useCase.replace(warehouse("MWH.REPLACE", "UNKNOWN", 15, 10)));

    store.create(warehouse("MWH.OTHER", "VETSBY-001", 10, 5));
    Warehouse candidate = warehouse("MWH.REPLACE", "VETSBY-001", 30, 10);
    assertThrows(WarehouseOperationException.class, () -> useCase.replace(candidate));

    store.create(warehouse("MWH.CAPACITY", "AMSTERDAM-001", 90, 5));
    Warehouse capacityExceeded = warehouse("MWH.REPLACE", "AMSTERDAM-001", 20, 10);
    assertThrows(WarehouseOperationException.class, () -> useCase.replace(capacityExceeded));
  }

  private Warehouse warehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static class TestWarehouseStore implements WarehouseStore {
    private final List<Warehouse> history = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return history.stream().filter(warehouse -> warehouse.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      history.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {
      history.remove(warehouse);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return getAll().stream()
          .filter(warehouse -> buCode.equals(warehouse.businessUnitCode))
          .findFirst()
          .orElse(null);
    }
  }
}
