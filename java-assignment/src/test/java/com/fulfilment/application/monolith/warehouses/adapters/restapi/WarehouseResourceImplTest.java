package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseOperationException;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@QuarkusTest
class WarehouseResourceImplTest {

  @Mock WarehouseRepository warehouseRepository;
  @Mock CreateWarehouseOperation createWarehouse;
  @Mock ArchiveWarehouseOperation archiveWarehouse;
  @Mock ReplaceWarehouseOperation replaceWarehouse;

  @InjectMocks WarehouseResourceImpl resource;

  private AutoCloseable mocks;

  @BeforeEach
  void initializeMocks() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void closeMocks() throws Exception {
    mocks.close();
  }

  @Test
  void listAllMapsEveryWarehouseFieldToTheApiModel() {
    Warehouse warehouse = warehouse(12L, "MWH.001", "AMSTERDAM-001", 80, 20);
    when(warehouseRepository.getAll()).thenReturn(List.of(warehouse));

    List<com.warehouse.api.beans.Warehouse> result = resource.listAllWarehousesUnits();

    assertEquals(1, result.size());
    assertEquals("12", result.get(0).getId());
    assertEquals("MWH.001", result.get(0).getBusinessUnitCode());
    assertEquals("AMSTERDAM-001", result.get(0).getLocation());
    assertEquals(80, result.get(0).getCapacity());
    assertEquals(20, result.get(0).getStock());
  }

  @Test
  void createMapsTheRequestToTheDomainAndReturnsTheCreatedValues() {
    com.warehouse.api.beans.Warehouse request = apiWarehouse("MWH.NEW", "VETSBY-001", 90, 10);

    com.warehouse.api.beans.Warehouse result = resource.createANewWarehouseUnit(request);

    assertNull(result.getId());
    assertEquals("MWH.NEW", result.getBusinessUnitCode());
    assertEquals("VETSBY-001", result.getLocation());
    assertEquals(90, result.getCapacity());
    assertEquals(10, result.getStock());
    verify(createWarehouse).create(any(Warehouse.class));
  }

  @Test
  void getsWarehousesByRepositoryIdentifierAndReportsMissingWarehouses() {
    when(warehouseRepository.findByIdentifier("MWH.001"))
        .thenReturn(warehouse(12L, "MWH.001", "AMSTERDAM-001", 80, 20));

    com.warehouse.api.beans.Warehouse result = resource.getAWarehouseUnitByID("MWH.001");

    assertEquals("12", result.getId());
    assertEquals("MWH.001", result.getBusinessUnitCode());
    when(warehouseRepository.findByIdentifier("missing")).thenReturn(null);
    WarehouseOperationException exception =
        assertThrows(
            WarehouseOperationException.class,
            () -> resource.getAWarehouseUnitByID("missing"));
    assertEquals(404, exception.getStatus());
  }

  @Test
  void archivesExistingWarehousesAndReportsMissingWarehouses() {
    Warehouse warehouse = warehouse(12L, "MWH.001", "AMSTERDAM-001", 80, 20);
    when(warehouseRepository.findByIdentifier("MWH.001")).thenReturn(warehouse);

    resource.archiveAWarehouseUnitByID("MWH.001");

    verify(archiveWarehouse).archive(warehouse);
    when(warehouseRepository.findByIdentifier("missing")).thenReturn(null);
    WarehouseOperationException exception =
        assertThrows(
            WarehouseOperationException.class,
            () -> resource.archiveAWarehouseUnitByID("missing"));
    assertEquals(404, exception.getStatus());
  }

  @Test
  void replaceUsesThePathBusinessCodeAndRejectsConflictingRequestCodes() {
    com.warehouse.api.beans.Warehouse request = apiWarehouse(null, "ZWOLLE-001", 20, 10);

    com.warehouse.api.beans.Warehouse result =
        resource.replaceTheCurrentActiveWarehouse("MWH.001", request);

    assertEquals("MWH.001", result.getBusinessUnitCode());
    verify(replaceWarehouse).replace(any(Warehouse.class));

    com.warehouse.api.beans.Warehouse conflictingRequest =
        apiWarehouse("MWH.OTHER", "ZWOLLE-001", 20, 10);
    WarehouseOperationException exception =
        assertThrows(
            WarehouseOperationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("MWH.001", conflictingRequest));
    assertEquals(400, exception.getStatus());
  }

  private Warehouse warehouse(
      Long id, String businessUnitCode, String location, Integer capacity, Integer stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.id = id;
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private com.warehouse.api.beans.Warehouse apiWarehouse(
      String businessUnitCode, String location, Integer capacity, Integer stock) {
    com.warehouse.api.beans.Warehouse warehouse = new com.warehouse.api.beans.Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation(location);
    warehouse.setCapacity(capacity);
    warehouse.setStock(stock);
    return warehouse;
  }
}
