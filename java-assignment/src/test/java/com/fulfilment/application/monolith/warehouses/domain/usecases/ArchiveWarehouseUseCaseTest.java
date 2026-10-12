package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  @Test
  public void archivesWarehouseBySettingTimestampAndUpdatingStore() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.ARCHIVE";
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(new WarehouseStore() {
      @Override
      public List<Warehouse> getAll() {
        return List.of();
      }

      @Override
      public void create(Warehouse ignored) {}

      @Override
      public void update(Warehouse updated) {
        assertNotNull(updated.archivedAt);
      }

      @Override
      public void remove(Warehouse ignored) {}

      @Override
      public Warehouse findByBusinessUnitCode(String code) {
        return null;
      }
    });

    useCase.archive(warehouse);
    assertNotNull(warehouse.archivedAt);
  }

  @Test
  public void rejectsNullOrPreviouslyArchivedWarehouse() {
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(new EmptyWarehouseStore());
    assertThrows(WarehouseOperationException.class, () -> useCase.archive(null));

    Warehouse archived = new Warehouse();
    archived.archivedAt = java.time.LocalDateTime.now();
    assertThrows(WarehouseOperationException.class, () -> useCase.archive(archived));
  }

  private static class EmptyWarehouseStore implements WarehouseStore {
    @Override
    public List<Warehouse> getAll() {
      return List.of();
    }

    @Override
    public void create(Warehouse warehouse) {}

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String code) {
      return null;
    }
  }
}
