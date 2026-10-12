package com.fulfilment.application.monolith.warehouses.domain.usecases;

public class WarehouseOperationException extends RuntimeException {

  private final int status;

  private WarehouseOperationException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static WarehouseOperationException badRequest(String message) {
    return new WarehouseOperationException(message, 400);
  }

  public static WarehouseOperationException notFound(String message) {
    return new WarehouseOperationException(message, 404);
  }

  public static WarehouseOperationException conflict(String message) {
    return new WarehouseOperationException(message, 409);
  }
}
