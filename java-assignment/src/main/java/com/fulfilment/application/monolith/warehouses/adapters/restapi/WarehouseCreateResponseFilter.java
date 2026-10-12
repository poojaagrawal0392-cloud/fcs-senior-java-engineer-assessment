package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class WarehouseCreateResponseFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext request, ContainerResponseContext response) throws IOException {
    String path = request.getUriInfo().getPath().replaceFirst("^/", "");
    if ("POST".equals(request.getMethod())
        && "warehouse".equals(path)
        && response.getStatus() == 200) {
      response.setStatus(201);
    }
  }
}
