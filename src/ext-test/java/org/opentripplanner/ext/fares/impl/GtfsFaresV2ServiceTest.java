package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  int ID = 100;
  String expressNetwork = "express";
  String localNetwork = "local";

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

  FareProduct localPass = new FareProduct(
    new FeedScopedId(FEED_ID, "local_pass"),
    "Local Pass",
    Money.euros(2000),
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
      new FareLegRule(FEED_ID, expressNetwork, null, null, expressPass),
      new FareLegRule(FEED_ID, localNetwork, null, null, localPass),
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
    assertEquals(Set.of(dayPass, single), result.itineraryProducts());
  }

  @Test
  void twoLegs() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).bus(ID, 55, 70, D).build();

    var result = service.getProducts(i1);
    assertEquals(Set.of(dayPass), result.itineraryProducts());
  }

  @Test
  void networkId() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).faresV2Rail(ID, 0, 50, C, expressNetwork).build();

    var result = service.getProducts(i1);
    assertEquals(Set.of(expressPass), result.itineraryProducts());
  }

  @Test
  void twoAreaIds() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, INNER_ZONE_STOP)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_STOP, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(Set.of(innerToOuterZoneSingle), result.itineraryProducts());
  }

  @Test
  void onlyToAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, B)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_STOP, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(Set.of(singleToOuter), result.itineraryProducts());
  }

  @Test
  void onlyFromAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, OUTER_ZONE_STOP)
      .faresV2Rail(ID, 0, 50, B, null)
      .build();

    var result = service.getProducts(i1);
    assertEquals(Set.of(singleFromOuter), result.itineraryProducts());
  }

  /**
   * Because we use both the local and the express network, there is no product covering both.
   */
  @Test
  void separateFares() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, A)
      .faresV2Rail(ID, 0, 50, B, localNetwork)
      .faresV2Rail(ID, 60, 100, C, expressNetwork)
      .build();

    var result = service.getProducts(i1);
    assertEquals(0, result.itineraryProducts().size());

    var localLeg = i1.getLegs().get(1);
    var localLegProducts = result.getProducts(localLeg);
    assertEquals(Set.of(localPass), localLegProducts);

    var expressLeg = i1.getLegs().get(2);
    var expressProducts = result.getProducts(expressLeg);
    assertEquals(Set.of(expressPass), expressProducts);
  }
}
