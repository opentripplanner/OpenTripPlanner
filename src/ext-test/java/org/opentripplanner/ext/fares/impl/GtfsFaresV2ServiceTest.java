package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  String LEG_GROUP1 = "leg-group1";
  String LEG_GROUP2 = "leg-group2";
  String LEG_GROUP3 = "leg-group3";
  String LEG_GROUP4 = "leg-group4";
  String LEG_GROUP5 = "leg-group5";
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

  FareProduct freeTransfer = new FareProduct(
    new FeedScopedId(FEED_ID, "free_transfer"),
    "Free transfer",
    Money.euros(0),
    null,
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
      new FareLegRule(LEG_GROUP1, null, null, null, single),
      new FareLegRule(LEG_GROUP1, null, null, OUTER_ZONE, singleToOuter),
      new FareLegRule(LEG_GROUP1, null, OUTER_ZONE, null, singleFromOuter),
      new FareLegRule(LEG_GROUP1, null, null, null, dayPass),
      new FareLegRule(LEG_GROUP1, expressNetwork, null, null, expressPass),
      new FareLegRule(LEG_GROUP1, localNetwork, null, null, localPass),
      new FareLegRule(LEG_GROUP1, null, INNER_ZONE, OUTER_ZONE, innerToOuterZoneSingle),
      new FareLegRule("another-leg-group", null, null, null, monthlyPass)
    ),
    List.of(),
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

  @Nested
  public class AreaId {

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
  }

  @Nested
  class Transfers {

    FareProduct freeTransferFromInnerToOuter = new FareProduct(
      new FeedScopedId(FEED_ID, "free-transfer-from-inner-to-outer"),
      "Single ticket with free transfer from the inner to the outer zone",
      Money.euros(500),
      null,
      null,
      null
    );

    FareProduct freeTransferSingle = new FareProduct(
      new FeedScopedId(FEED_ID, "free-transfer-from-anywhere-to-outer"),
      "Single ticket with free transfer any zone",
      Money.euros(1000),
      null,
      null,
      null
    );

    GtfsFaresV2Service service = new GtfsFaresV2Service(
      List.of(
        new FareLegRule(LEG_GROUP2, null, INNER_ZONE, INNER_ZONE, freeTransferFromInnerToOuter),
        new FareLegRule(LEG_GROUP3, null, OUTER_ZONE, OUTER_ZONE, single),
        new FareLegRule(LEG_GROUP4, null, null, null, freeTransferSingle),
        new FareLegRule(LEG_GROUP5, null, INNER_ZONE, OUTER_ZONE, singleToOuter)
      ),
      List.of(
        new FareTransferRule(LEG_GROUP1, LEG_GROUP1, 1, null, freeTransfer),
        new FareTransferRule(LEG_GROUP2, LEG_GROUP3, 1, null, freeTransfer),
        new FareTransferRule(LEG_GROUP4, LEG_GROUP4, 1, null, freeTransfer),
        new FareTransferRule(null, LEG_GROUP5, 1, null, freeTransfer)
      ),
      Multimaps.forMap(
        Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
      )
    );

    @Test
    void freeTransferInSameGroup() {
      var i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).bus(ID, 55, 70, D).build();
      var result = service.getProducts(i1);
      assertEquals(Set.of(freeTransferSingle), result.itineraryProducts());
    }

    @Test
    void freeTransferIntoAnotherGroup() {
      var i1 = newItinerary(A, 0)
        .walk(20, INNER_ZONE_STOP)
        .bus(ID, 0, 50, INNER_ZONE_STOP)
        .walk(53, OUTER_ZONE_STOP)
        .bus(ID, 55, 70, OUTER_ZONE_STOP)
        .build();
      var result = service.getProducts(i1);
      assertEquals(Set.of(freeTransferFromInnerToOuter), result.itineraryProducts());
    }
  }
}
