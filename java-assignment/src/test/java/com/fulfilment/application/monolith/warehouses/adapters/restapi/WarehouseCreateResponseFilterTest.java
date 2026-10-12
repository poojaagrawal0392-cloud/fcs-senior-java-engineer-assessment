package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.UriInfo;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseCreateResponseFilterTest {

  private final WarehouseCreateResponseFilter filter = new WarehouseCreateResponseFilter();

  @Test
  void changesSuccessfulWarehousePostToCreated() throws Exception {
    ContainerRequestContext request = request("POST", "/warehouse");
    ContainerResponseContext response = mock(ContainerResponseContext.class);
    when(response.getStatus()).thenReturn(200);

    filter.filter(request, response);

    verify(response).setStatus(201);
  }

  @Test
  void leavesOtherResponsesUnchanged() throws Exception {
    assertUnchanged(request("GET", "/warehouse"), 200);
    assertUnchanged(request("POST", "/warehouse/1"), 200);
    assertUnchanged(request("POST", "/warehouse"), 201);
  }

  private void assertUnchanged(ContainerRequestContext request, int status) throws Exception {
    ContainerResponseContext response = mock(ContainerResponseContext.class);
    when(response.getStatus()).thenReturn(status);

    filter.filter(request, response);

    verify(response, never()).setStatus(201);
  }

  private ContainerRequestContext request(String method, String path) {
    ContainerRequestContext request = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn(path);
    return request;
  }
}
