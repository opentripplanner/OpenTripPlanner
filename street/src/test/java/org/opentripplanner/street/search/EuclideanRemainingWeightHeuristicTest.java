package org.opentripplanner.street.search;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelDetails;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.StreetLimitationParametersService;

class EuclideanRemainingWeightHeuristicTest {

  public static Stream<Arguments> testCases() {
    var safeStreets = new StreetModelDetails(
      StreetConstants.DEFAULT_MAX_CAR_SPEED,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      0.6f,
      0.8f
    );
    var unsafeStreets = new StreetModelDetails(
      StreetConstants.DEFAULT_MAX_CAR_SPEED,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      2,
      2
    );
    var slowCar = new StreetModelDetails(1.0f, StreetConstants.DEFAULT_MAX_AREA_NODES, 1, 1);
    return Stream.of(
      // default walk speed = 1.33, walk reluctance = 2
      Arguments.argumentSet(
        "default walk",
        StreetModelDetails.DEFAULT,
        StreetSearchRequest.DEFAULT,
        150.38
      ),
      // default bike speed = 5, bike reluctance = 2
      Arguments.argumentSet(
        "default bike",
        StreetModelDetails.DEFAULT,
        StreetSearchRequest.of().withMode(StreetMode.BIKE).build(),
        40
      ),
      // default car speed = 40
      Arguments.argumentSet(
        "default car",
        StreetModelDetails.DEFAULT,
        StreetSearchRequest.of().withMode(StreetMode.CAR).build(),
        2.5
      ),
      Arguments.argumentSet(
        "slow walk",
        StreetModelDetails.DEFAULT,
        StreetSearchRequest.of()
          .withWalk(w -> w.withSpeed(1))
          .build(),
        200
      ),
      Arguments.argumentSet(
        "slow preferred walk",
        StreetModelDetails.DEFAULT,
        StreetSearchRequest.of()
          .withWalk(w -> w.withSpeed(1).withReluctance(0.5))
          .build(),
        50
      ),
      Arguments.argumentSet(
        "slow preferred safe walk",
        safeStreets,
        StreetSearchRequest.of()
          .withWalk(w -> w.withSpeed(1).withReluctance(0.5))
          .build(),
        40
      ),
      Arguments.argumentSet(
        "partial walk safety",
        safeStreets,
        StreetSearchRequest.of()
          .withWalk(w -> w.withSpeed(1).withReluctance(0.5).withSafetyFactor(0.2))
          .build(),
        48
      ),
      Arguments.argumentSet(
        "slow preferred unsafe walk",
        unsafeStreets,
        StreetSearchRequest.of()
          .withWalk(w -> w.withSpeed(1).withReluctance(0.5))
          .build(),
        100
      ),
      // safe bike
      Arguments.argumentSet(
        "safe bike",
        safeStreets,
        StreetSearchRequest.of().withMode(StreetMode.BIKE).build(),
        24
      ),
      Arguments.argumentSet(
        "bike triangle",
        safeStreets,
        StreetSearchRequest.of()
          .withMode(StreetMode.BIKE)
          .withBike(b ->
            b
              .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
              .withOptimizeTriangle(t -> t.withSafety(0.75).withTime(0.25))
          )
          .build(),
        28
      ),
      // a slow car
      Arguments.argumentSet(
        "slow car",
        slowCar,
        StreetSearchRequest.of().withMode(StreetMode.CAR).build(),
        100
      ),
      // slow car speed should not affect cycling
      Arguments.argumentSet(
        "slow car",
        slowCar,
        StreetSearchRequest.of().withMode(StreetMode.BIKE).build(),
        40
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void estimateRemainingWeight(
    StreetModelDetails streetModelDetails,
    StreetSearchRequest req,
    double expected
  ) {
    var subject = new EuclideanRemainingWeightHeuristic(
      new StreetLimitationParametersService() {
        @Override
        public float maxCarSpeed() {
          return streetModelDetails.maxCarSpeed();
        }

        @Override
        public int maxAreaNodes() {
          return streetModelDetails.maxAreaNodes();
        }

        @Override
        public float getBestWalkSafety() {
          return streetModelDetails.bestWalkSafety();
        }

        @Override
        public float getBestBikeSafety() {
          return streetModelDetails.bestBikeSafety();
        }
      }
    );
    var fromVertex = new SimpleVertex("origin", 0, 0);
    var toCoordinate = SphericalDistanceLibrary.moveMeters(new WgsCoordinate(0, 0), 100, 0);
    var toVertex = new SimpleVertex(
      "destination",
      toCoordinate.latitude(),
      toCoordinate.longitude()
    );

    var state = new State(fromVertex, req);

    subject.initialize(Set.of(toVertex), req);
    Assertions.assertEquals(expected, subject.estimateRemainingWeight(state), 0.5);
  }
}
