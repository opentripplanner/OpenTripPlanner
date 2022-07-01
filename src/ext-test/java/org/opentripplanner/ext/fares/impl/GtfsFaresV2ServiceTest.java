package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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

  FareProduct single = new FareProduct(
    new FeedScopedId(FEED_ID, "single"),
    "Single one-way ticket",
    Money.euros(100),
    null
  );
  FareProduct dayPass = new FareProduct(
    new FeedScopedId(FEED_ID, "day_pass"),
    "Day Pass",
    Money.euros(500),
    Duration.ofDays(1)
  );
  FareProduct monthlyPass = new FareProduct(
    new FeedScopedId("another", "monthly_pass"),
    "Monthly Pass",
    Money.euros(3000),
    Duration.ofDays(30)
  );
  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      new FareLegRule(FEED_ID, null, single),
      new FareLegRule(FEED_ID, null, dayPass),
      new FareLegRule("another-feed", null, monthlyPass)
    )
  );

  @Test
  void singleLeg() {
    var ID = 100;
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TransitModelForTest.stopForTest("1:stop", 1d, 1d)))
      .bus(ID, 0, 50, B)
      .build();

    var result = service.getProducts(i1);
    assertEquals(List.of(single, dayPass), result.productsCoveringItinerary().stream().toList());
  }

  @Test
  void twoLegs() {
    var ID = 100;
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TransitModelForTest.stopForTest("1:stop", 1d, 1d)))
      .bus(ID, 0, 50, B)
      .bus(ID, 55, 70, B)
      .build();

    var result = service.getProducts(i1);
    assertEquals(List.of(dayPass), result.productsCoveringItinerary().stream().toList());
  }
}
