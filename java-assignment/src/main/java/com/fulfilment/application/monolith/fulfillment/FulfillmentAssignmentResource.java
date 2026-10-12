package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/fulfillments")
@ApplicationScoped
@Consumes("application/json")
@Produces("application/json")
public class FulfillmentAssignmentResource {

  private final WarehouseRepository warehouseRepository;
  private final ProductRepository productRepository;

  public FulfillmentAssignmentResource(
      WarehouseRepository warehouseRepository, ProductRepository productRepository) {
    this.warehouseRepository = warehouseRepository;
    this.productRepository = productRepository;
  }

  @POST
  @Transactional
  public Response assign(FulfillmentRequest request) {
    if (request == null || request.storeId == null || request.productId == null
        || request.warehouseBusinessUnitCode == null
        || request.warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException("Store, product, and warehouse are required.", 400);
    }
    if (Store.findById(request.storeId) == null) {
      throw new WebApplicationException("Store was not found.", 404);
    }
    if (productRepository.findById(request.productId) == null) {
      throw new WebApplicationException("Product was not found.", 404);
    }
    if (warehouseRepository.findByBusinessUnitCode(request.warehouseBusinessUnitCode) == null) {
      throw new WebApplicationException("Active warehouse was not found.", 404);
    }

    List<FulfillmentAssignment> storeAssignments =
        FulfillmentAssignment.list("storeId", request.storeId);
    List<FulfillmentAssignment> productAssignments =
        FulfillmentAssignment.list("storeId = ?1 and productId = ?2",
            request.storeId, request.productId);
    List<FulfillmentAssignment> warehouseAssignments =
        FulfillmentAssignment.list("warehouseBusinessUnitCode",
            request.warehouseBusinessUnitCode);

    if (storeAssignments.stream().anyMatch(assignment ->
        assignment.productId.equals(request.productId)
            && assignment.warehouseBusinessUnitCode.equals(request.warehouseBusinessUnitCode))) {
      throw new WebApplicationException("This fulfillment assignment already exists.", 409);
    }
    if (distinctValues(productAssignments, assignment -> assignment.warehouseBusinessUnitCode)
        .size() >= 2) {
      throw new WebApplicationException(
          "A product can be fulfilled by at most two warehouses per store.", 409);
    }
    if (!distinctValues(storeAssignments, assignment -> assignment.warehouseBusinessUnitCode)
            .contains(request.warehouseBusinessUnitCode)
        && distinctValues(storeAssignments, assignment -> assignment.warehouseBusinessUnitCode)
                .size()
            >= 3) {
      throw new WebApplicationException("A store can use at most three fulfillment warehouses.", 409);
    }
    if (!distinctValues(warehouseAssignments, assignment -> assignment.productId)
            .contains(request.productId)
        && distinctValues(warehouseAssignments, assignment -> assignment.productId).size() >= 5) {
      throw new WebApplicationException("A warehouse can store at most five product types.", 409);
    }

    FulfillmentAssignment assignment = new FulfillmentAssignment();
    assignment.storeId = request.storeId;
    assignment.productId = request.productId;
    assignment.warehouseBusinessUnitCode = request.warehouseBusinessUnitCode;
    assignment.persist();
    return Response.status(Response.Status.CREATED).entity(assignment).build();
  }

  private <T> Set<T> distinctValues(
      List<FulfillmentAssignment> assignments,
      java.util.function.Function<FulfillmentAssignment, T> value) {
    return assignments.stream().map(value).collect(Collectors.toSet());
  }

  public static class FulfillmentRequest {
    public Long storeId;
    public Long productId;
    public String warehouseBusinessUnitCode;

    public FulfillmentRequest() {}
  }
}
