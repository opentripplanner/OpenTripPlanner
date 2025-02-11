package org.opentripplanner.ext.fares.impl;

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
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareDistance.LinearDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  FeedScopedId LEG_GROUP1 = id("leg-group1");
  FeedScopedId LEG_GROUP2 = id("leg-group2");
  FeedScopedId LEG_GROUP3 = id("leg-group3");
  FeedScopedId LEG_GROUP4 = id("leg-group4");
  FeedScopedId LEG_GROUP5 = id("leg-group5");
  int ID = 100;
  String expressNetwork = "express";
  String localNetwork = "local";

  FareProduct single = FareProduct
    .of(new FeedScopedId(FEED_ID, "single"), "Single one-way ticket", Money.euros(1))
    .build();
  FareProduct singleToOuter = FareProduct
    .of(
      new FeedScopedId(FEED_ID, "single_to_outer"),
      "Single one-way ticket to outer zone",
      Money.euros(1)
    )
    .build();
  FareProduct singleFromOuter = FareProduct
    .of(
      new FeedScopedId(FEED_ID, "single_from_outer"),
      "Single one-way ticket from outer zone to anywhere",
      Money.euros(1)
    )
    .build();
  FareProduct dayPass = FareProduct
    .of(new FeedScopedId(FEED_ID, "day_pass"), "Day Pass", Money.euros(5))
    .withValidity(Duration.ofDays(1))
    .build();
  FareProduct innerToOuterZoneSingle = FareProduct
    .of(new FeedScopedId(FEED_ID, "zone_ab_single"), "Day Pass", Money.euros(5))
    .build();
  FareProduct monthlyPass = FareProduct
    .of(new FeedScopedId("another", "monthly_pass"), "Monthly Pass", Money.euros(30))
    .withValidity(Duration.ofDays(30))
    .build();
  FareProduct expressPass = FareProduct
    .of(new FeedScopedId(FEED_ID, "express_pass"), "Express Pass", Money.euros(50))
    .withValidity(Duration.ofDays(1))
    .build();
  FareProduct localPass = FareProduct
    .of(new FeedScopedId(FEED_ID, "local_pass"), "Local Pass", Money.euros(20))
    .withValidity(Duration.ofDays(1))
    .build();
  FareProduct freeTransfer = FareProduct
    .of(new FeedScopedId(FEED_ID, "free_transfer"), "Free transfer", Money.euros(0))
    .build();

  Place INNER_ZONE_STOP = Place.forStop(
    testModel.stop("inner city stop").withCoordinate(1, 1).build()
  );
  Place OUTER_ZONE_STOP = Place.forStop(
    testModel.stop("outer city stop").withCoordinate(2, 2).build()
  );
  String INNER_ZONE = "inner-zone";
  String OUTER_ZONE = "outer-zone";

  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("1"), single).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule
        .of(id("2"), singleToOuter)
        .withLegGroupId(LEG_GROUP1)
        .withToAreaId(OUTER_ZONE)
        .build(),
      FareLegRule
        .of(id("3"), singleFromOuter)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(id("4"), dayPass).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule
        .of(id("5"), expressPass)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(expressNetwork)
        .build(),
      FareLegRule
        .of(id("5"), localPass)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(localNetwork)
        .build(),
      FareLegRule
        .of(id("6"), innerToOuterZoneSingle)
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

    FeedScopedId TRANSFER_ID = id("transfer");
    FareProduct freeTransferFromInnerToOuter = FareProduct
      .of(
        new FeedScopedId(FEED_ID, "free-transfer-from-inner-to-outer"),
        "Single ticket with free transfer from the inner to the outer zone",
        Money.euros(50)
      )
      .build();

    FareProduct freeTransferSingle = FareProduct
      .of(
        new FeedScopedId(FEED_ID, "free-transfer-from-anywhere-to-outer"),
        "Single ticket with free transfer any zone",
        Money.euros(10)
      )
      .build();

    GtfsFaresV2Service service = new GtfsFaresV2Service(
      List.of(
        FareLegRule
          .of(id("6"), freeTransferFromInnerToOuter)
          .withLegGroupId(LEG_GROUP2)
          .withFromAreaId(INNER_ZONE)
          .withToAreaId(INNER_ZONE)
          .build(),
        FareLegRule
          .of(id("7"), single)
          .withLegGroupId(LEG_GROUP3)
          .withFromAreaId(OUTER_ZONE)
          .withToAreaId(OUTER_ZONE)
          .build(),
        FareLegRule.of(id("8"), freeTransferSingle).withLegGroupId(LEG_GROUP4).build(),
        FareLegRule
          .of(id("9"), singleToOuter)
          .withLegGroupId(LEG_GROUP5)
          .withFromAreaId(INNER_ZONE)
          .withToAreaId(OUTER_ZONE)
          .build()
      ),
      List.of(
        new FareTransferRule(TRANSFER_ID, LEG_GROUP1, LEG_GROUP1, 1, null, List.of(freeTransfer)),
        new FareTransferRule(TRANSFER_ID, LEG_GROUP2, LEG_GROUP3, 1, null, List.of(freeTransfer)),
        new FareTransferRule(TRANSFER_ID, LEG_GROUP4, LEG_GROUP4, 1, null, List.of(freeTransfer)),
        new FareTransferRule(TRANSFER_ID, null, LEG_GROUP5, 1, null, List.of(freeTransfer))
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

  @Nested
  class DistanceFares {

    FeedScopedId DISTANCE_ID = id("distance");
    FareProduct threeStopProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "three-stop-product"), "three-stop-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();
    FareProduct fiveStopProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "five-stop-product"), "five-stop-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();
    FareProduct twelveStopProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "twelve-stop-product"), "twelve-stop-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();

    FareProduct tenKmProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "ten-km-product"), "ten-km-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();
    FareProduct threeKmProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "three-km-product"), "three-km-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();
    FareProduct twoKmProduct = FareProduct
      .of(new FeedScopedId(FEED_ID, "two-km-product"), "two-km-product", Money.euros(1))
      .withValidity(Duration.ofHours(1))
      .build();

    List<FareLegRule> stopRules = List.of(
      FareLegRule
        .of(DISTANCE_ID, threeStopProduct)
        .withFareDistance(new FareDistance.Stops(0, 3))
        .build(),
      FareLegRule
        .of(DISTANCE_ID, fiveStopProduct)
        .withFareDistance(new FareDistance.Stops(5, 10))
        .build(),
      FareLegRule
        .of(DISTANCE_ID, twelveStopProduct)
        .withFareDistance(new FareDistance.Stops(12, 20))
        .build()
    );

    List<FareLegRule> distanceRules = List.of(
      FareLegRule
        .of(DISTANCE_ID, tenKmProduct)
        .withFareDistance(
          new LinearDistance(
            Distance.ofKilometersBoxed(7d, ignore -> {}).orElse(null),
            Distance.ofKilometersBoxed(10d, ignore -> {}).orElse(null)
          )
        )
        .build(),
      FareLegRule
        .of(DISTANCE_ID, threeKmProduct)
        .withFareDistance(
          new LinearDistance(
            Distance.ofKilometersBoxed(3d, ignore -> {}).orElse(null),
            Distance.ofKilometersBoxed(6d, ignore -> {}).orElse(null)
          )
        )
        .build(),
      FareLegRule
        .of(DISTANCE_ID, twoKmProduct)
        .withFareDistance(
          new LinearDistance(
            Distance.ofMetersBoxed(0d, ignore -> {}).orElse(null),
            Distance.ofMetersBoxed(2000d, ignore -> {}).orElse(null)
          )
        )
        .build()
    );

    @Test
    void stops() {
      var i1 = newItinerary(A, 0).bus(ID, 0, 50, 1, 20, C).build();
      var faresV2Service = new GtfsFaresV2Service(
        stopRules,
        List.of(),
        Multimaps.forMap(
          Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
        )
      );
      assertEquals(faresV2Service.getProducts(i1).itineraryProducts(), Set.of(twelveStopProduct));
    }

    @Test
    void directDistance() {
      Place dest = testModel.place(
        "Destination",
        A.coordinate.latitude(),
        A.coordinate.longitude() + SphericalDistanceLibrary.metersToDegrees(5_000)
      );
      var i1 = newItinerary(A, 0).bus(ID, 0, 50, dest).build();
      var faresV2Service = new GtfsFaresV2Service(
        distanceRules,
        List.of(),
        Multimaps.forMap(
          Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
        )
      );
      assertEquals(faresV2Service.getProducts(i1).itineraryProducts(), Set.of(threeKmProduct));
    }
  }
}
