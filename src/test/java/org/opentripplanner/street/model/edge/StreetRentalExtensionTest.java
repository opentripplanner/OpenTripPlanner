package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class StreetRentalExtensionTest {

  StreetVertex V1 = intersectionVertex("V1", 0.0, 0.0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  String network = "tier-oslo";

  @Test
  public void dontLeaveBusinessArea() {
    var edge = StreetModelForTest.streetEdge(V1, V2);
    edge.addRentalExtension(new StreetEdgeRentalExtension.BusinessAreaBorder(network));
    State result = traverse(edge);
    assertNull(result);
  }

  @Test
  public void dontEnterGeofencingZone() {
    var edge = StreetModelForTest.streetEdge(V1, V2);
    edge.addRentalExtension(
      new StreetEdgeRentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, true)
      )
    );
    State result = traverse(edge);
    assertNull(result);
  }

  @Test
  public void dontFinishInNoDropOffZone() {
    var edge = StreetModelForTest.streetEdge(V1, V2);
    edge.addRentalExtension(
      new StreetEdgeRentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );
    State result = traverse(edge);
    assertFalse(result.isFinal());
  }

  @Test
  public void finishInEdgeWithoutRestrictions() {
    var edge = StreetModelForTest.streetEdge(V1, V2);
    State result = traverse(edge);
    assertTrue(result.isFinal());
  }

  private State traverse(StreetEdge edge) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(V1, req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    return edge.traverse(editor.makeState());
  }
}
