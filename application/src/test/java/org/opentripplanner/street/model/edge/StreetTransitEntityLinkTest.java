package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model.basic.Accessibility.NOT_POSSIBLE;
import static org.opentripplanner.transit.model.basic.Accessibility.NO_INFORMATION;
import static org.opentripplanner.transit.model.basic.Accessibility.POSSIBLE;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

class StreetTransitEntityLinkTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final RegularStop ACCESSIBLE_STOP = stopForTest(
    "A:accessible",
    "wheelchair accessible stop",
    10.001,
    10.001,
    null,
    POSSIBLE
  );

  @Nested
  class WheelchairAccessibility {

    static final RegularStop inaccessibleStop = stopForTest(
      "A:inaccessible",
      "wheelchair inaccessible stop",
      10.001,
      10.001,
      null,
      NOT_POSSIBLE
    );

    static final RegularStop unknownStop = stopForTest(
      "A:unknown",
      "unknown",
      10.001,
      10.001,
      null,
      NO_INFORMATION
    );

    @Test
    void disallowInaccessibleStop() {
      var afterTraversal = traverse(inaccessibleStop, true);
      assertTrue(State.isEmpty(afterTraversal));
    }

    @Test
    void allowAccessibleStop() {
      var afterTraversal = traverse(ACCESSIBLE_STOP, true);

      assertFalse(State.isEmpty(afterTraversal));
    }

    @Test
    void unknownStop() {
      var afterTraversal = traverse(unknownStop, false);
      assertFalse(State.isEmpty(afterTraversal));

      var afterStrictTraversal = traverse(unknownStop, true);
      assertTrue(State.isEmpty(afterStrictTraversal));
    }

    private State[] traverse(RegularStop stop, boolean onlyAccessible) {
      var from = StreetModelForTest.intersectionVertex("A", 10, 10);
      var to = TransitStopVertex.of().withStop(stop).withModes(Set.of(TransitMode.RAIL)).build();

      var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
      AccessibilityPreferences feature;
      if (onlyAccessible) {
        feature = AccessibilityPreferences.ofOnlyAccessible();
      } else {
        feature = AccessibilityPreferences.ofCost(100, 100);
      }
      req.withWheelchair(true);
      req.withPreferences(p ->
        p.withWheelchair(
          WheelchairPreferences.of()
            .withTrip(feature)
            .withStop(feature)
            .withElevator(feature)
            .withInaccessibleStreetReluctance(25)
            .withMaxSlope(0.045)
            .withSlopeExceededReluctance(10)
            .withStairsReluctance(25)
            .build()
        )
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
      var transitStopVertex = TransitStopVertex.of().withStop(ACCESSIBLE_STOP).build();
      var edge = StreetTransitStopLink.createStreetTransitStopLink(
        (StreetVertex) state.getVertex(),
        transitStopVertex
      );
      var result = edge.traverse(state);
      assertEquals(canTraverse, result.length > 0);
    }
  }

  private static RegularStop stopForTest(
    String idAndName,
    String desc,
    double lat,
    double lon,
    Station parent,
    Accessibility wheelchair
  ) {
    return TEST_MODEL.stop(idAndName)
      .withDescription(NonLocalizedString.ofNullable(desc))
      .withCoordinate(new WgsCoordinate(lat, lon))
      .withWheelchairAccessibility(wheelchair)
      .withParentStation(parent)
      .build();
  }
}
