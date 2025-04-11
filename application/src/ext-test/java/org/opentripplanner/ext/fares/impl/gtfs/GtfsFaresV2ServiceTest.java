package org.opentripplanner.ext.fares.impl.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();

  private static final FeedScopedId LEG_GROUP1 = id("leg-group1");
  private static final int ID = 100;
  private static final FeedScopedId expressNetwork = id("express");
  private static final FeedScopedId localNetwork = id("local");

  private static final FareProduct single = FareProduct.of(
    new FeedScopedId(FEED_ID, "single"),
    "Single one-way ticket",
    Money.euros(1)
  ).build();
  private static final FareProduct singleToOuter = FareProduct.of(
    new FeedScopedId(FEED_ID, "single_to_outer"),
    "Single one-way ticket to outer zone",
    Money.euros(1)
  ).build();
  private static final FareProduct singleFromOuter = FareProduct.of(
    new FeedScopedId(FEED_ID, "single_from_outer"),
    "Single one-way ticket from outer zone to anywhere",
    Money.euros(1)
  ).build();
  private static final FareProduct dayPass = FareProduct.of(
    new FeedScopedId(FEED_ID, "day_pass"),
    "Day Pass",
    Money.euros(5)
  )
    .withValidity(Duration.ofDays(1))
    .build();
  private static final FareProduct innerToOuterZoneSingle = FareProduct.of(
    new FeedScopedId(FEED_ID, "zone_ab_single"),
    "Day Pass",
    Money.euros(5)
  ).build();
  private static final FareProduct monthlyPass = FareProduct.of(
    new FeedScopedId("another", "monthly_pass"),
    "Monthly Pass",
    Money.euros(30)
  )
    .withValidity(Duration.ofDays(30))
    .build();
  private static final FareProduct expressPass = FareProduct.of(
    new FeedScopedId(FEED_ID, "express_pass"),
    "Express Pass",
    Money.euros(50)
  )
    .withValidity(Duration.ofDays(1))
    .build();
  private static final FareProduct localPass = FareProduct.of(
    new FeedScopedId(FEED_ID, "local_pass"),
    "Local Pass",
    Money.euros(20)
  )
    .withValidity(Duration.ofDays(1))
    .build();

  private static final Place INNER_ZONE_STOP = Place.forStop(
    MODEL.stop("inner city stop").withCoordinate(1, 1).build()
  );
  private static final Place OUTER_ZONE_STOP = Place.forStop(
    MODEL.stop("outer city stop").withCoordinate(2, 2).build()
  );
  private static final FeedScopedId INNER_ZONE = id("inner-zone");
  private static final FeedScopedId OUTER_ZONE = id("outer-zone");

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("1"), single).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule.of(id("2"), singleToOuter)
        .withLegGroupId(LEG_GROUP1)
        .withToAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(id("3"), singleFromOuter)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(id("4"), dayPass).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule.of(id("5"), expressPass)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(expressNetwork)
        .build(),
      FareLegRule.of(id("5"), localPass)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(localNetwork)
        .build(),
      FareLegRule.of(id("6"), innerToOuterZoneSingle)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(INNER_ZONE)
        .withToAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(monthlyPass.id(), monthlyPass).withLegGroupId(id("another-leg-group")).build()
    ),
    List.of(),
    Multimaps.forMap(
      Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
    )
  );

  @Test
  void singleLeg() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(Set.of(dayPass, single), result.productsForLeg(i1.legs().get(1)));
  }

  @Test
  void twoLegs() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).bus(ID, 55, 70, D).build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(Set.of(dayPass, single), result.productsForLeg(i1.legs().get(1)));
  }

  @Test
  void networkId() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).faresV2Rail(ID, 0, 50, C, expressNetwork).build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(Set.of(expressPass), result.productsForLeg(i1.legs().get(1)));
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

    var result = SERVICE.calculateFares(i1);
    assertEquals(0, result.itineraryProducts().size());

    var localLeg = i1.legs().get(1);
    var localLegProducts = result.productsForLeg(localLeg);
    assertEquals(Set.of(localPass), localLegProducts);

    var expressLeg = i1.legs().get(2);
    var expressProducts = result.productsForLeg(expressLeg);
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

      var result = SERVICE.calculateFares(i1);
      assertEquals(Set.of(innerToOuterZoneSingle), result.productsForLeg(i1.legs().get(1)));
    }

    @Test
    void onlyToAreaId() {
      Itinerary i1 = newItinerary(A, 0)
        .walk(20, B)
        .faresV2Rail(ID, 0, 50, OUTER_ZONE_STOP, null)
        .build();

      var result = SERVICE.calculateFares(i1);
      assertEquals(Set.of(singleToOuter), result.productsForLeg(i1.legs().get(1)));
    }

    @Test
    void onlyFromAreaId() {
      Itinerary i1 = newItinerary(A, 0)
        .walk(20, OUTER_ZONE_STOP)
        .faresV2Rail(ID, 0, 50, B, null)
        .build();

      var result = SERVICE.calculateFares(i1);
      assertEquals(Set.of(singleFromOuter), result.productsForLeg(i1.legs().get(1)));
    }
  }
}
