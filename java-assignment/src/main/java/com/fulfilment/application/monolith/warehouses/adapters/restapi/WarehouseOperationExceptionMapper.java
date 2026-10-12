package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseOperationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WarehouseOperationExceptionMapper
    implements ExceptionMapper<WarehouseOperationException> {

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(WarehouseOperationException exception) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("exceptionType", exception.getClass().getName());
    body.put("code", exception.getStatus());
    body.put("error", exception.getMessage());
    return Response.status(exception.getStatus()).entity(body).build();
  }
}
