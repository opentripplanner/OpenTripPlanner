package org.opentripplanner.ext.fares.impl._support;

import static org.opentripplanner.ext.fares.impl._support.FareModelForTest.fareProduct;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.groupOfRoutes;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;

public interface FareTestConstants {
  FareProduct FARE_PRODUCT_A = fareProduct("A");
  FareProduct FARE_PRODUCT_B = fareProduct("B");
  FareProduct TRANSFER_1 = FareProduct.of(id("transfer:1"), "transfer 1", Money.euros(1)).build();

  GroupOfRoutes NETWORK_A = groupOfRoutes("A").build();
  GroupOfRoutes NETWORK_B = groupOfRoutes("B").build();

  FeedScopedId LEG_GROUP_A = id("LG-A");
  FeedScopedId LEG_GROUP_B = id("LG-B");
}
