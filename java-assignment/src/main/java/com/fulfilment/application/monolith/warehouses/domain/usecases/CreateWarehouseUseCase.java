package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  public CreateWarehouseUseCase(WarehouseStore warehouseStore) {
    this(warehouseStore, new com.fulfilment.application.monolith.location.LocationGateway());
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null
        || warehouse.businessUnitCode.isBlank()) {
      throw WarehouseOperationException.badRequest("Business unit code is required.");
    }
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw WarehouseOperationException.conflict(
          "An active warehouse already uses business unit code " + warehouse.businessUnitCode + ".");
    }
    validateCapacityAndStock(warehouse);
    Location location = resolveLocation(warehouse.location);
    validateLocationCapacity(warehouse, location, null);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;
    warehouseStore.create(warehouse);
  }

  static void validateCapacityAndStock(Warehouse warehouse) {
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw WarehouseOperationException.badRequest("Warehouse capacity must be greater than zero.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw WarehouseOperationException.badRequest("Warehouse stock cannot be negative or missing.");
    }
    if (warehouse.stock > warehouse.capacity) {
      throw WarehouseOperationException.badRequest("Warehouse stock cannot exceed its capacity.");
    }
  }

  Location resolveLocation(String identifier) {
    Location location = locationResolver.resolveByIdentifier(identifier);
    if (location == null) {
      throw WarehouseOperationException.badRequest("The specified location does not exist.");
    }
    return location;
  }

  void validateLocationCapacity(Warehouse warehouse, Location location, String excludedBusinessUnitCode) {
    var activeWarehouses = warehouseStore.getAll().stream()
        .filter(existing -> Objects.equals(existing.location, warehouse.location))
        .filter(existing -> !Objects.equals(existing.businessUnitCode, excludedBusinessUnitCode))
        .toList();
    if (activeWarehouses.size() >= location.maxNumberOfWarehouses) {
      throw WarehouseOperationException.badRequest(
          "The maximum number of warehouses for this location has been reached.");
    }
    long totalCapacity = activeWarehouses.stream()
        .mapToLong(existing -> existing.capacity == null ? 0L : existing.capacity)
        .sum();
    if (totalCapacity + warehouse.capacity > location.maxCapacity) {
      throw WarehouseOperationException.badRequest(
          "Warehouse capacity exceeds the remaining capacity at this location.");
    }
  }
}
