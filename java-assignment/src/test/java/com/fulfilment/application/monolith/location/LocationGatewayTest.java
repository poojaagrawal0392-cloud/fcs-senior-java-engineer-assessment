package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void resolvesExistingLocation() {
    Location location = new LocationGateway().resolveByIdentifier("ZWOLLE-001");

    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void returnsNullForUnknownOrNullIdentifier() {
    LocationGateway gateway = new LocationGateway();

    assertNull(gateway.resolveByIdentifier("UNKNOWN"));
    assertNull(gateway.resolveByIdentifier(null));
  }
}
