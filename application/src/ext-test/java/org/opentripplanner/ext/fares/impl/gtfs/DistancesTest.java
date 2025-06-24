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
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DistancesTest {

  private static final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private static final Place INNER_ZONE_STOP = Place.forStop(
    testModel.stop("inner city stop").withCoordinate(1, 1).build()
  );
  private static final Place OUTER_ZONE_STOP = Place.forStop(
    testModel.stop("outer city stop").withCoordinate(2, 2).build()
  );
  FeedScopedId INNER_ZONE = id("inner-zone");
  FeedScopedId OUTER_ZONE = id("outer-zone");

  private static final int ID = 100;
  FeedScopedId DISTANCE_ID = id("distance");
  FareProduct threeStopProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "three-stop-product"),
    "three-stop-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();
  FareProduct fiveStopProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "five-stop-product"),
    "five-stop-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();
  FareProduct twelveStopProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "twelve-stop-product"),
    "twelve-stop-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();

  FareProduct tenKmProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "ten-km-product"),
    "ten-km-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();
  FareProduct threeKmProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "three-km-product"),
    "three-km-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();
  FareProduct twoKmProduct = FareProduct.of(
    new FeedScopedId(FEED_ID, "two-km-product"),
    "two-km-product",
    Money.euros(1)
  )
    .withValidity(Duration.ofHours(1))
    .build();

  List<FareLegRule> stopRules = List.of(
    FareLegRule.of(DISTANCE_ID, threeStopProduct)
      .withFareDistance(new FareDistance.Stops(0, 3))
      .build(),
    FareLegRule.of(DISTANCE_ID, fiveStopProduct)
      .withFareDistance(new FareDistance.Stops(5, 10))
      .build(),
    FareLegRule.of(DISTANCE_ID, twelveStopProduct)
      .withFareDistance(new FareDistance.Stops(12, 20))
      .build()
  );

  List<FareLegRule> distanceRules = List.of(
    FareLegRule.of(DISTANCE_ID, tenKmProduct)
      .withFareDistance(
        new FareDistance.LinearDistance(
          Distance.ofKilometersBoxed(7d, ignore -> {}).orElse(null),
          Distance.ofKilometersBoxed(10d, ignore -> {}).orElse(null)
        )
      )
      .build(),
    FareLegRule.of(DISTANCE_ID, threeKmProduct)
      .withFareDistance(
        new FareDistance.LinearDistance(
          Distance.ofKilometersBoxed(3d, ignore -> {}).orElse(null),
          Distance.ofKilometersBoxed(6d, ignore -> {}).orElse(null)
        )
      )
      .build(),
    FareLegRule.of(DISTANCE_ID, twoKmProduct)
      .withFareDistance(
        new FareDistance.LinearDistance(
          Distance.ofMetersBoxed(0d, ignore -> {}).orElse(null),
          Distance.ofMetersBoxed(2000d, ignore -> {}).orElse(null)
        )
      )
      .build()
  );

  @Test
  void stops() {
    var i1 = newItinerary(PlanTestConstants.A, 0)
      .bus(ID, 0, 50, 1, 20, PlanTestConstants.C)
      .build();
    var faresV2Service = new GtfsFaresV2Service(
      stopRules,
      List.of(),
      Multimaps.forMap(
        Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
      )
    );
    assertEquals(
      faresV2Service.calculateFares(i1).offersForLeg(i1.legs().getLast()),
      Set.of(FareOffer.of(i1.listScheduledTransitLegs().getFirst().startTime(), twelveStopProduct))
    );
  }

  @Test
  void directDistance() {
    Place dest = testModel.place(
      "Destination",
      PlanTestConstants.A.coordinate.latitude(),
      PlanTestConstants.A.coordinate.longitude() + SphericalDistanceLibrary.metersToDegrees(5_000)
    );
    var i1 = newItinerary(PlanTestConstants.A, 0).bus(ID, 0, 50, dest).build();
    var faresV2Service = new GtfsFaresV2Service(
      distanceRules,
      List.of(),
      Multimaps.forMap(
        Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
      )
    );
    assertEquals(
      faresV2Service.calculateFares(i1).offersForLeg(i1.transitLeg(0)),
      Set.of(FareOffer.of(i1.listScheduledTransitLegs().getFirst().startTime(), threeKmProduct))
    );
  }
}
