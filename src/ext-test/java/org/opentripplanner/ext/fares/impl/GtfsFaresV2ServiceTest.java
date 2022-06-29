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

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  FareProduct dayPass = new FareProduct("day_pass", "Day Pass", Money.euros(500));
  FareProduct monthlyPass = new FareProduct("monthly_pass", "Monthly Pass", Money.euros(3000));

  @Test
  void getCost() {
    var ID = 100;
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TransitModelForTest.stopForTest("1:stop", 1d, 1d)))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    var service = new GtfsFaresV2Service(
      List.of(
        new FareLegRule(FEED_ID, "all routes", null, dayPass),
        new FareLegRule("another-feed", "all routes", null, monthlyPass)
      )
    );

    var fare = service.getCost(i1);
    assertEquals(1, fare.getProducts().size());

    var product = fare.getProducts().stream().toList().get(0);
    assertEquals("day_pass", product.id());
    assertEquals(Money.euros(500), product.amount());
  }
}
