package org.opentripplanner.ext.fares.model;

import static org.opentripplanner.ext.fares.model.FareModelForTest.fareProduct;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.groupOfRoutes;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalTime;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.network.GroupOfRoutes;

public interface FareTestConstants {
  FareProduct FARE_PRODUCT_A = fareProduct("A");
  FareProduct FARE_PRODUCT_B = fareProduct("B");
  FareProduct TRANSFER_1 = FareProduct.of(id("transfer:1"), "transfer 1", Money.euros(1)).build();

  GroupOfRoutes NETWORK_A = groupOfRoutes("A").build();
  GroupOfRoutes NETWORK_B = groupOfRoutes("B").build();

  FeedScopedId LEG_GROUP_A = id("LG-A");
  FeedScopedId LEG_GROUP_B = id("LG-B");
  Timeframe TIMEFRAME_TWELVE_TO_TWO = Timeframe.of()
    .withServiceId(FeedScopedIdForTestFactory.id("s1"))
    .withStart(LocalTime.of(12, 0))
    .withEnd(LocalTime.of(14, 0))
    .build();

  Timeframe TIMEFRAME_THREE_TO_FIVE = Timeframe.of()
    .withServiceId(FeedScopedIdForTestFactory.id("s1"))
    .withStart(LocalTime.of(15, 0))
    .withEnd(LocalTime.of(17, 0))
    .build();

  Timeframe TIMEFRAME_ALL_DAY = Timeframe.of()
    .withServiceId(FeedScopedIdForTestFactory.id("s2"))
    .withStart(LocalTime.MIN)
    .withEnd(LocalTime.MAX)
    .build();
}
