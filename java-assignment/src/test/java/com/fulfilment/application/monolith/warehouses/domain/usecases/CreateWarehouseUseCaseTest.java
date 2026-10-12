package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private TestWarehouseStore store;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = new TestWarehouseStore();
    useCase = new CreateWarehouseUseCase(store, new LocationGateway());
  }

  @Test
  public void createsValidWarehouseAndSetsCreationTimestamp() {
    Warehouse warehouse = warehouse("MWH.NEW", "VETSBY-001", 80, 20);

    useCase.create(warehouse);

    assertEquals(1, store.warehouses.size());
    assertNotNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  public void rejectsMissingCodeAndDuplicateActiveCode() {
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse(" ", "VETSBY-001", 80, 20)));

    store.warehouses.add(warehouse("MWH.EXISTING", "ZWOLLE-001", 20, 10));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.EXISTING", "VETSBY-001", 80, 20)));
  }

  @Test
  public void rejectsUnknownLocationAndInvalidStockOrCapacity() {
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.BADLOC", "UNKNOWN", 10, 1)));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.BADCAP", "VETSBY-001", 0, 0)));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.BADSTOCK", "VETSBY-001", 10, 11)));
  }

  @Test
  public void enforcesLocationWarehouseCountAndAggregateCapacity() {
    store.warehouses.add(warehouse("MWH.ZWOLLE", "ZWOLLE-001", 20, 5));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.ZWOLLE2", "ZWOLLE-001", 10, 5)));

    store.warehouses.clear();
    store.warehouses.add(warehouse("MWH.AMS", "AMSTERDAM-001", 90, 5));
    assertThrows(WarehouseOperationException.class,
        () -> useCase.create(warehouse("MWH.AMS2", "AMSTERDAM-001", 20, 5)));
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
    private final List<Warehouse> warehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return warehouses.stream().filter(warehouse -> warehouse.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {
      warehouses.remove(warehouse);
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
