package com.fulfilment.application.monolith.stores;

import org.junit.jupiter.api.Test;

class LegacyStoreManagerGatewayTest {

  @Test
  void writesStoreForCreateAndUpdateNotifications() {
    LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();
    Store store = new Store("gateway-test-store");
    store.quantityProductsInStock = 4;

    gateway.createStoreOnLegacySystem(store);
    gateway.updateStoreOnLegacySystem(store);
  }
}
