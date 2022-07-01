package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  FareProduct dayPass = new FareProduct(
    new FeedScopedId(FEED_ID, "day_pass"),
    "Day Pass",
    Money.euros(500),
    null
  );
  FareProduct monthlyPass = new FareProduct(
    new FeedScopedId("another", "monthly_pass"),
    "Monthly Pass",
    Money.euros(3000),
    null
  );

  @Test
  void feedIdBasedFare() {
    var ID = 100;
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TransitModelForTest.stopForTest("1:stop", 1d, 1d)))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    var service = new GtfsFaresV2Service(
      List.of(
        new FareLegRule(FEED_ID, null, dayPass),
        new FareLegRule("another-feed", null, monthlyPass)
      )
    );

    var fare = service.getProducts(i1);
    assertEquals(1, fare.size());

    var product = fare.stream().toList().get(0);
    assertEquals("day_pass", product.id().getId());
    assertEquals(Money.euros(500), product.amount());
  }
}
