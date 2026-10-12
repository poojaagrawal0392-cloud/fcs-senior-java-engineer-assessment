package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseOperationException;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouse;
  @Inject private ArchiveWarehouseOperation archiveWarehouse;
  @Inject private ReplaceWarehouseOperation replaceWarehouse;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomain(data);
    createWarehouse.create(warehouse);
    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        warehouseRepository.findByIdentifier(id);
    if (warehouse == null) {
      throw WarehouseOperationException.notFound("Warehouse " + id + " was not found.");
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        warehouseRepository.findByIdentifier(id);
    if (warehouse == null) {
      throw WarehouseOperationException.notFound("Warehouse " + id + " was not found.");
    }
    archiveWarehouse.archive(warehouse);
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    if (data.getBusinessUnitCode() != null
        && !businessUnitCode.equals(data.getBusinessUnitCode())) {
      throw WarehouseOperationException.badRequest(
          "Replacement business unit code must match the path value.");
    }
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomain(data);
    warehouse.businessUnitCode = businessUnitCode;
    replaceWarehouse.replace(warehouse);
    return toWarehouseResponse(warehouse);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomain(
      com.warehouse.api.beans.Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    if (warehouse.id != null) {
      response.setId(warehouse.id.toString());
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
