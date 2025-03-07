package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.CARGO_BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER_SEATED;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER_STANDING;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class StreetEdgeRentalTraversalTest {

  StreetVertex v0 = intersectionVertex(0.0, 0.0);
  StreetVertex v1 = intersectionVertex(2.0, 2.0);

  private static Stream<Arguments> baseCases(StreetTraversalPermission p) {
    return Stream.of(
      of(SCOOTER, SCOOTER_RENTAL, p),
      of(SCOOTER_SEATED, SCOOTER_RENTAL, p),
      of(SCOOTER_STANDING, SCOOTER_RENTAL, p),
      of(BICYCLE, BIKE_RENTAL, p),
      of(CARGO_BICYCLE, BIKE_RENTAL, p)
    );
  }

  static Stream<Arguments> allowedToTraverse() {
    return Stream.of(
      StreetTraversalPermission.ALL,
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      StreetTraversalPermission.BICYCLE
    ).flatMap(StreetEdgeRentalTraversalTest::baseCases);
  }

  @ParameterizedTest(
    name = "Form factor {0}, street mode {1} should be able to traverse edge with permission {2}"
  )
  @MethodSource("allowedToTraverse")
  void scooterBicycleTraversal(
    RentalFormFactor formFactor,
    StreetMode streetMode,
    StreetTraversalPermission permission
  ) {
    StreetEdge e0 = streetEdge(v0, v1, 50.0, permission);
    var req = StreetSearchRequest.of().withMode(streetMode).withArriveBy(false).build();

    var editor = new StateEditor(v0, req);
    editor.beginFloatingVehicleRenting(formFactor, "network", false);
    var state = editor.makeState();

    assertEquals(state.currentMode(), formFactor.traverseMode);
    var states = e0.traverse(state);
    assertEquals(1, states.length);
    var afterTraversal = states[0];

    assertEquals(formFactor.traverseMode, afterTraversal.currentMode());
  }

  static Stream<Arguments> noTraversal() {
    return Stream.of(StreetTraversalPermission.CAR, StreetTraversalPermission.NONE).flatMap(
      StreetEdgeRentalTraversalTest::baseCases
    );
  }

  @ParameterizedTest(
    name = "Form factor {0}, street mode {1} should not be able to traverse edge with permission {2}"
  )
  @MethodSource("noTraversal")
  void noTraversal(
    RentalFormFactor formFactor,
    StreetMode streetMode,
    StreetTraversalPermission permission
  ) {
    StreetEdge e0 = streetEdge(v0, v1, 50.0, permission);
    var req = StreetSearchRequest.of().withMode(streetMode).withArriveBy(false).build();

    var editor = new StateEditor(v0, req);
    editor.beginFloatingVehicleRenting(formFactor, "network", false);
    var state = editor.makeState();

    assertEquals(state.currentMode(), formFactor.traverseMode);
    var afterTraversal = e0.traverse(state);

    assertTrue(State.isEmpty(afterTraversal));
  }
}
