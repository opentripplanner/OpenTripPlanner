package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

  int ID = 100;
  String express = "express";

  FareProduct single = new FareProduct(
    new FeedScopedId(FEED_ID, "single"),
    "Single one-way ticket",
    Money.euros(100),
    null,
    null,
    null
  );
  FareProduct singleToOuter = new FareProduct(
    new FeedScopedId(FEED_ID, "single_to_outer"),
    "Single one-way ticket to outer zone",
    Money.euros(100),
    null,
    null,
    null
  );

  FareProduct singleFromOuter = new FareProduct(
    new FeedScopedId(FEED_ID, "single_from_outer"),
    "Single one-way ticket from outer zone to anywhere",
    Money.euros(100),
    null,
    null,
    null
  );

  FareProduct dayPass = new FareProduct(
    new FeedScopedId(FEED_ID, "day_pass"),
    "Day Pass",
    Money.euros(500),
    Duration.ofDays(1),
    null,
    null
  );
  FareProduct innerToOuterZoneSingle = new FareProduct(
    new FeedScopedId(FEED_ID, "zone_ab_single"),
    "Day Pass",
    Money.euros(500),
    null,
    null,
    null
  );
  FareProduct monthlyPass = new FareProduct(
    new FeedScopedId("another", "monthly_pass"),
    "Monthly Pass",
    Money.euros(3000),
    Duration.ofDays(30),
    null,
    null
  );

  FareProduct expressPass = new FareProduct(
    new FeedScopedId(FEED_ID, "express_pass"),
    "Express Pass",
    Money.euros(5000),
    Duration.ofDays(1),
    null,
    null
  );

  Place INNER_ZONE_STOP = Place.forStop(
    TransitModelForTest.stop("inner city stop").withCoordinate(1, 1).build()
  );
  Place OUTER_ZONE_STOP = Place.forStop(
    TransitModelForTest.stop("outer city stop").withCoordinate(2, 2).build()
  );
  String INNER_ZONE = "inner-zone";
  String OUTER_ZONE = "outer-zone";

  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      new FareLegRule(FEED_ID, null, null, null, single),
      new FareLegRule(FEED_ID, null, null, OUTER_ZONE, singleToOuter),
      new FareLegRule(FEED_ID, null, OUTER_ZONE, null, singleFromOuter),
      new FareLegRule(FEED_ID, null, null, null, dayPass),
      new FareLegRule(FEED_ID, express, null, null, expressPass),
      new FareLegRule(FEED_ID, null, INNER_ZONE, OUTER_ZONE, innerToOuterZoneSingle),
      new FareLegRule("another-feed", null, null, null, monthlyPass)
    ),
    Multimaps.forMap(
      Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
    )
  );

  @Test
  void singleLeg() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).build();

    var result = service.getProducts(i1);
    assertEquals(List.of(single, dayPass), result.productsCoveringItinerary());
  }

  @Test
  void twoLegs() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).bus(ID, 55, 70, D).build();

    var result = service.getProducts(i1);
    assertEquals(List.of(dayPass), result.productsCoveringItinerary());
  }

  @Test
  void networkId() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).faresV2Rail(ID, 0, 50, C, express).build();

    var result = service.getProducts(i1);
    assertEquals(List.of(expressPass), result.productsCoveringItinerary());
  }

  @Test
  void twoAreaIds() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, INNER_ZONE_STOP)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_STOP, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(List.of(innerToOuterZoneSingle), result.productsCoveringItinerary());
  }

  @Test
  void onlyToAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, B)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_STOP, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(List.of(singleToOuter), result.productsCoveringItinerary());
  }

  @Test
  void onlyFromAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, OUTER_ZONE_STOP)
      .faresV2Rail(ID, 0, 50, B, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(List.of(singleFromOuter), result.productsCoveringItinerary());
  }
}
