package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore) {
    this(warehouseStore, new com.fulfilment.application.monolith.location.LocationGateway());
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null || newWarehouse.businessUnitCode == null
        || newWarehouse.businessUnitCode.isBlank()) {
      throw WarehouseOperationException.badRequest("Business unit code is required.");
    }
    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw WarehouseOperationException.notFound(
          "Active warehouse with business unit code " + newWarehouse.businessUnitCode + " was not found.");
    }
    if (!Objects.equals(current.stock, newWarehouse.stock)) {
      throw WarehouseOperationException.badRequest(
          "Replacement warehouse stock must match the current warehouse stock.");
    }
    if (current.stock == null) {
      throw WarehouseOperationException.badRequest(
          "The current warehouse stock must be available to replace the warehouse.");
    }
    if (newWarehouse.capacity != null && newWarehouse.capacity < current.stock) {
      throw WarehouseOperationException.badRequest(
          "Replacement capacity cannot be less than the current warehouse stock.");
    }
    CreateWarehouseUseCase.validateCapacityAndStock(newWarehouse);
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw WarehouseOperationException.badRequest("The specified location does not exist.");
    }
    var activeWarehousesAtLocation = warehouseStore.getAll().stream()
        .filter(existing -> Objects.equals(existing.location, newWarehouse.location))
        .filter(existing -> !Objects.equals(existing.businessUnitCode, current.businessUnitCode))
        .toList();
    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw WarehouseOperationException.badRequest(
          "The maximum number of warehouses for this location has been reached.");
    }
    long remainingCapacity = location.maxCapacity - activeWarehousesAtLocation.stream()
        .mapToLong(existing -> existing.capacity == null ? 0L : existing.capacity)
        .sum();
    if (newWarehouse.capacity > remainingCapacity) {
      throw WarehouseOperationException.badRequest(
          "Warehouse capacity exceeds the remaining capacity at this location.");
    }

    current.archivedAt = java.time.LocalDateTime.now();
    warehouseStore.update(current);
    newWarehouse.createdAt = java.time.LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
