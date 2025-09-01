package org.opentripplanner.street.search.strategy;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;

class EuclideanRemainingWeightHeuristicTest {

  public static Stream<Arguments> testCases() {
    var safeStreets = new StreetLimitationParameters();
    safeStreets.initBestBikeSafety(0.6f);
    safeStreets.initBestWalkSafety(0.8f);

    var unsafeStreets = new StreetLimitationParameters();
    unsafeStreets.initBestWalkSafety(2);
    unsafeStreets.initBestBikeSafety(2);

    var slowCar = new StreetLimitationParameters();
    slowCar.initMaxCarSpeed(1);

    return Stream.of(
      // default walk speed = 1.33, walk reluctance = 2
      Arguments.argumentSet(
        "default walk",
        new StreetLimitationParameters(),
        StreetMode.WALK,
        RoutingPreferences.DEFAULT,
        150.38
      ),
      // default bike speed = 5, bike reluctance = 2
      Arguments.argumentSet(
        "default bike",
        new StreetLimitationParameters(),
        StreetMode.BIKE,
        RoutingPreferences.DEFAULT,
        40
      ),
      // default car speed = 40
      Arguments.argumentSet(
        "default car",
        new StreetLimitationParameters(),
        StreetMode.CAR,
        RoutingPreferences.DEFAULT,
        2.5
      ),
      Arguments.argumentSet(
        "slow walk",
        new StreetLimitationParameters(),
        StreetMode.WALK,
        RoutingPreferences.of().withWalk(w -> w.withSpeed(1)).build(),
        200
      ),
      Arguments.argumentSet(
        "slow preferred walk",
        new StreetLimitationParameters(),
        StreetMode.WALK,
        RoutingPreferences.of().withWalk(w -> w.withSpeed(1).withReluctance(0.5)).build(),
        50
      ),
      Arguments.argumentSet(
        "slow preferred safe walk",
        safeStreets,
        StreetMode.WALK,
        RoutingPreferences.of().withWalk(w -> w.withSpeed(1).withReluctance(0.5)).build(),
        40
      ),
      Arguments.argumentSet(
        "partial walk safety",
        safeStreets,
        StreetMode.WALK,
        RoutingPreferences.of()
          .withWalk(w -> w.withSpeed(1).withReluctance(0.5).withSafetyFactor(0.2))
          .build(),
        48
      ),
      Arguments.argumentSet(
        "slow preferred unsafe walk",
        unsafeStreets,
        StreetMode.WALK,
        RoutingPreferences.of().withWalk(w -> w.withSpeed(1).withReluctance(0.5)).build(),
        100
      ),
      // safe bike
      Arguments.argumentSet(
        "safe bike",
        safeStreets,
        StreetMode.BIKE,
        RoutingPreferences.DEFAULT,
        24
      ),
      Arguments.argumentSet(
        "bike triangle",
        safeStreets,
        StreetMode.BIKE,
        RoutingPreferences.of()
          .withBike(b ->
            b
              .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
              .withOptimizeTriangle(t -> t.withSafety(0.75).withTime(0.25))
          )
          .build(),
        28
      ),
      // safest bike
      Arguments.argumentSet(
        "safest bike",
        safeStreets,
        StreetMode.BIKE,
        RoutingPreferences.of()
          .withBike(b -> b.withOptimizeType(VehicleRoutingOptimizeType.SAFEST_STREETS))
          .build(),
        24
      ),
      // a slow car
      Arguments.argumentSet("slow car", slowCar, StreetMode.CAR, RoutingPreferences.DEFAULT, 100),
      // slow car speed should not affect cycling
      Arguments.argumentSet("slow car", slowCar, StreetMode.BIKE, RoutingPreferences.DEFAULT, 40)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void estimateRemainingWeight(
    StreetLimitationParameters streetLimitationParameters,
    StreetMode streetMode,
    RoutingPreferences preferences,
    double expected
  ) {
    var subject = new EuclideanRemainingWeightHeuristic(
      new DefaultStreetLimitationParametersService(streetLimitationParameters)
    );
    var fromVertex = new SimpleVertex("origin", 0, 0);
    var toCoordinate = SphericalDistanceLibrary.moveMeters(new WgsCoordinate(0, 0), 100, 0);
    var toVertex = new SimpleVertex(
      "destination",
      toCoordinate.latitude(),
      toCoordinate.longitude()
    );

    var state = new State(
      fromVertex,
      StreetSearchRequest.of().withMode(streetMode).withPreferences(preferences).build()
    );

    subject.initialize(streetMode, Set.of(toVertex), false, preferences);
    Assertions.assertEquals(expected, subject.estimateRemainingWeight(state), 0.5);
  }
}
