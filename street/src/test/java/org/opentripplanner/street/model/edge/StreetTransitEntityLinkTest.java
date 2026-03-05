package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.accessibility.Accessibility.NOT_POSSIBLE;
import static org.opentripplanner.core.model.accessibility.Accessibility.NO_INFORMATION;
import static org.opentripplanner.core.model.accessibility.Accessibility.POSSIBLE;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.AccessibilityRequest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StreetTransitEntityLinkTest {

  private record Stop(String id, String name, Point coordinate, Accessibility wheelchair) {}

  private static final Stop ACCESSIBLE_STOP = stop("A:accessible", POSSIBLE);

  @Nested
  class WheelchairAccessibility {

    static final Stop INACCESSIBLE_STOP = stop("A:inaccessible", NOT_POSSIBLE);

    static final Stop UNKNOWN_STOP = stop("A:unknown", NO_INFORMATION);

    @Test
    void disallowInaccessibleStop() {
      var afterTraversal = traverse(INACCESSIBLE_STOP, true);
      assertTrue(State.isEmpty(afterTraversal));
    }

    @Test
    void allowAccessibleStop() {
      var afterTraversal = traverse(ACCESSIBLE_STOP, true);

      assertFalse(State.isEmpty(afterTraversal));
    }

    @Test
    void unknownStop() {
      var afterTraversal = traverse(UNKNOWN_STOP, false);
      assertFalse(State.isEmpty(afterTraversal));

      var afterStrictTraversal = traverse(UNKNOWN_STOP, true);
      assertTrue(State.isEmpty(afterStrictTraversal));
    }

    private State[] traverse(Stop stop, boolean onlyAccessible) {
      var from = StreetModelFactory.intersectionVertex("A", 10, 10);
      var to = TransitStopVertex.of()
        .withId(id(stop.id))
        .withPoint(stop.coordinate)
        .withWheelchairAccessiblity(stop.wheelchair)
        .build();

      var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
      AccessibilityRequest feature;
      if (onlyAccessible) {
        feature = AccessibilityRequest.ofOnlyAccessible();
      } else {
        feature = AccessibilityRequest.ofCost(100, 100);
      }
      req.withWheelchairEnabled(true);
      req.withWheelchair(b ->
        b
          .withTrip(feature)
          .withStop(feature)
          .withElevator(feature)
          .withInaccessibleStreetReluctance(25)
          .withMaxSlope(0.045)
          .withSlopeExceededReluctance(10)
          .withStairsReluctance(25)
          .build()
      );

      var edge = StreetTransitStopLink.createStreetTransitStopLink(from, to);
      return edge.traverse(new State(from, req.build()));
    }
  }

  @Nested
  class Rental {

    static List<State> allowedStates() {
      return Stream.of(
        TestStateBuilder.ofScooterRental().pickUpFreeFloatingScooter(),
        TestStateBuilder.ofBikeRental().pickUpFreeFloatingBike(),
        // allowing cars into stations is a bit questionable but the alternatives would be quite
        // computationally expensive
        TestStateBuilder.ofCarRental().pickUpFreeFloatingCar(),
        TestStateBuilder.ofWalking(),
        TestStateBuilder.ofCycling()
      )
        .map(TestStateBuilder::build)
        .toList();
    }

    @ParameterizedTest
    @MethodSource("allowedStates")
    void freeFloatingVehiclesAreAllowedIntoStops(State state) {
      testTraversalWithState(state, true);
    }

    static List<State> notAllowedStates() {
      return Stream.of(
        TestStateBuilder.ofBikeRental().pickUpBikeFromStation(),
        TestStateBuilder.ofCarRental().pickUpCarFromStation(),
        // for bike and ride you need to drop the bike at a parking facility first
        TestStateBuilder.ofBikeAndRide().streetEdge(),
        TestStateBuilder.parkAndRide().streetEdge()
      )
        .map(TestStateBuilder::build)
        .toList();
    }

    @ParameterizedTest
    @MethodSource("notAllowedStates")
    void stationBasedVehiclesAreNotAllowedIntoStops(State state) {
      testTraversalWithState(state, false);
    }

    private void testTraversalWithState(State state, boolean canTraverse) {
      var transitStopVertex = TransitStopVertex.of()
        .withId(id(ACCESSIBLE_STOP.id()))
        .withPoint(ACCESSIBLE_STOP.coordinate())
        .withWheelchairAccessiblity(ACCESSIBLE_STOP.wheelchair())
        .build();
      var edge = StreetTransitStopLink.createStreetTransitStopLink(
        (StreetVertex) state.getVertex(),
        transitStopVertex
      );
      var result = edge.traverse(state);
      assertEquals(canTraverse, result.length > 0);
    }
  }

  private static Stop stop(String idAndName, Accessibility wheelchair) {
    return new Stop(
      idAndName,
      idAndName,
      GeometryUtils.getGeometryFactory().createPoint(new Coordinate(10.001, 10.001)),
      wheelchair
    );
  }
}
