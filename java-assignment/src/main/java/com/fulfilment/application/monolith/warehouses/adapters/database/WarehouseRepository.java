package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    copyToEntity(warehouse, entity);
    persist(entity);
    warehouse.id = entity.id;
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null",
            warehouse.businessUnitCode)
        .firstResult();
    if (entity == null) {
      throw new IllegalStateException(
          "No active warehouse exists for business unit code " + warehouse.businessUnitCode + ".");
    }
    copyToEntity(warehouse, entity);
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode = ?1", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", buCode)
        .firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  public Warehouse findByIdentifier(String identifier) {
    if (identifier == null) {
      return null;
    }
    Optional<Long> numericId = parseId(identifier);
    if (numericId.isPresent()) {
      DbWarehouse entity = findById(numericId.get());
      if (entity != null) {
        return entity.archivedAt == null ? entity.toWarehouse() : null;
      }
    }
    return findByBusinessUnitCode(identifier);
  }

  private Optional<Long> parseId(String identifier) {
    try {
      return Optional.of(Long.parseLong(identifier));
    } catch (NumberFormatException ignored) {
      return Optional.empty();
    }
  }

  private void copyToEntity(Warehouse warehouse, DbWarehouse entity) {
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
  }
}
