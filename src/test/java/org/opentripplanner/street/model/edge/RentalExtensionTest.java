package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.street.model.vertex.RentalExtension;
import org.opentripplanner.street.model.vertex.RentalExtension.BusinessAreaBorder;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RentalExtensionTest {

  String network = "tier-oslo";
  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  StreetVertex V3 = intersectionVertex("V3", 2, 2);
  StreetVertex V4 = intersectionVertex("V4", 3, 3);

  @Test
  public void leaveBusinessAreaOnFoot() {
    var edge = streetEdge(V1, V2);
    edge.addTraversalExtension(new BusinessAreaBorder(network));
    State result = traverse(edge);
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
    assertEquals(TraverseMode.WALK, result.getBackMode());
    assertNull(result.getNextResult());
  }

  @Test
  public void dontEnterGeofencingZoneOnFoot() {
    var edge = streetEdge(V1, V2);
    edge.addTraversalExtension(
      new RentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, true)
      )
    );
    State result = traverse(edge);
    assertEquals(WALK, result.getBackMode());
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
  }

  @Test
  public void forkStateWhenEnteringNoDropOffZone() {
    var edge1 = streetEdge(V4, V1);
    var edge2 = streetEdge(V2, V3);
    var restrictedEdge = streetEdge(V1, V2);

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(edge1.getFromVertex(), req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    restrictedEdge.addTraversalExtension(
      new RentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );

    var continueOnFoot = edge1.traverse(editor.makeState());

    assertEquals(HAVE_RENTED, continueOnFoot.getVehicleRentalState());
    assertEquals(WALK, continueOnFoot.getBackMode());

    var continueRenting = continueOnFoot.getNextResult();
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
    assertEquals(BICYCLE, continueRenting.getBackMode());
    assertTrue(continueRenting.isInsideNoRentalDropOffArea());

    var insideZone = restrictedEdge.traverse(continueRenting);

    var leftNoDropOff = edge2.traverse(insideZone);
    assertFalse(leftNoDropOff.isInsideNoRentalDropOffArea());
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
  }

  @Test
  public void dontFinishInNoDropOffZone() {
    var edge = streetEdge(V1, V2);
    edge.addTraversalExtension(
      new RentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );
    State result = traverse(edge);
    assertFalse(result.isFinal());
  }

  @Test
  public void finishInEdgeWithoutRestrictions() {
    var edge = streetEdge(V1, V2);
    State result = traverse(edge);
    assertTrue(result.isFinal());
  }

  @Test
  public void addTwoExtensions() {
    var edge = streetEdge(V1, V2);
    edge.addTraversalExtension(new BusinessAreaBorder("a"));
    edge.addTraversalExtension(new BusinessAreaBorder("b"));

    assertTrue(edge.fromv.traversalBanned(state("a")));
    assertTrue(edge.fromv.traversalBanned(state("b")));
  }

  @Test
  public void removeExtensions() {
    var edge = streetEdge(V1, V2);
    final BusinessAreaBorder a = new BusinessAreaBorder("a");
    final BusinessAreaBorder b = new BusinessAreaBorder("b");
    final BusinessAreaBorder c = new BusinessAreaBorder("c");

    edge.addTraversalExtension(a);

    assertTrue(edge.fromv.traversalExtension().traversalBanned(state("a")));

    edge.addTraversalExtension(b);
    edge.addTraversalExtension(c);

    edge.removeTraversalExtension(a);

    assertTrue(edge.fromv.traversalExtension().traversalBanned(state("b")));
    assertTrue(edge.fromv.traversalExtension().traversalBanned(state("c")));

    edge.removeTraversalExtension(b);

    assertTrue(edge.fromv.traversalExtension().traversalBanned(state("c")));
  }

  @Test
  public void checkNetwork() {
    var edge = streetEdge(V1, V2);
    edge.addTraversalExtension(new BusinessAreaBorder("a"));

    var state = traverse(edge);

    assertEquals(RENTING_FLOATING, state.getVehicleRentalState());
    assertNull(state.getNextResult());
  }

  private State traverse(StreetEdge edge) {
    var state = state(network);
    return edge.traverse(state);
  }

  @Nonnull
  private State state(String network) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(V1, req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    return editor.makeState();
  }
}
