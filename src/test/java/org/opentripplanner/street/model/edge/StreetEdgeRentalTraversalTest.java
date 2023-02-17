package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor.BICYCLE;
import static org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor.CARGO_BICYCLE;
import static org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor.SCOOTER;
import static org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor.SCOOTER_SEATED;
import static org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor.SCOOTER_STANDING;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.test.support.VariableSource;

public class StreetEdgeRentalTraversalTest {

  StreetVertex v0 = intersectionVertex(0.0, 0.0);
  StreetVertex v1 = intersectionVertex(2.0, 2.0);

  static Stream<Arguments> rentalCases = Stream.of(
    of(SCOOTER, SCOOTER_RENTAL),
    of(SCOOTER_SEATED, SCOOTER_RENTAL),
    of(SCOOTER_STANDING, SCOOTER_RENTAL),
    of(BICYCLE, BIKE_RENTAL),
    of(CARGO_BICYCLE, BIKE_RENTAL)
  );

  @ParameterizedTest(name = "Form factor {0}, street mode {1} should be able to traverse")
  @VariableSource("rentalCases")
  public void scooterBicycleTraversal(
    RentalVehicleType.FormFactor formFactor,
    StreetMode streetMode
  ) {
    StreetEdge e0 = streetEdge(v0, v1, 50.0, StreetTraversalPermission.BICYCLE);
    var req = StreetSearchRequest.of().withMode(streetMode).withArriveBy(false).build();

    var editor = new StateEditor(v0, req);
    editor.beginFloatingVehicleRenting(formFactor, "network", false);
    var state = editor.makeState();

    assertEquals(state.getNonTransitMode(), formFactor.traverseMode);
    var afterTraversal = e0.traverse(state);

    assertNotNull(afterTraversal);

    assertEquals(formFactor.traverseMode, afterTraversal.getNonTransitMode());
  }
}
