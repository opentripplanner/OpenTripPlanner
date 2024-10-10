package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model.edge.StreetEdgeGeofencingTest.NETWORK_TIER;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class EdgeStateTest {
  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);

  @Test
  public void testReverseBatteryMetersWithoutBackState() {
    var edge1 = streetEdge(V1, V2);
    var results = reverseFromV1WithoutBackState(edge1);

    assertEquals(results.traversedBatteryMeters, 0);
    assertEquals(results.currentRangeMeters, Double.POSITIVE_INFINITY);
  }

  @Test
  public void testReverseBatteryMetersWithoutBatteryDistance() {
    var edge1 = streetEdge(V1, V2);
    var results = reverseFromV1WithoutBatteryDistance(edge1);

    assertEquals(results.traversedBatteryMeters, 0);
    assertEquals(results.currentRangeMeters, Double.POSITIVE_INFINITY);
  }

  @Test
  public void testReverseBatteryMetersWithBatteryDistance() {
    var edge1 = streetEdge(V1, V2);
    var results = reverseFromV1WithBatteryDistance(edge1);

    assertEquals(results.traversedBatteryMeters, 350);
    assertEquals(results.currentRangeMeters, 1000);
  }

  private State reverseFromV1WithoutBackState(StreetEdge edge) {
    var state = initialStateWithoutBackState(V1, NETWORK_TIER, false);
    return state.reverse();
  }

  private State reverseFromV1WithoutBatteryDistance(StreetEdge edge) {
    var state = initialStateWithoutBatteryDistance(V1, NETWORK_TIER, false);
    return state.reverse();
  }

  private State reverseFromV1WithBatteryDistance(StreetEdge edge) {
    var state = initialStateWithBatteryDistance(V1, NETWORK_TIER, false);
    state.currentRangeMeters = 2000;
    state.traversedBatteryMeters = 600;
    return state.reverse();
  }




  @Nonnull
  private State initialStateWithoutBackState(Vertex startVertex, String network, boolean arriveBy) {
    var req = StreetSearchRequest
      .of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
    var editor = new StateEditor(startVertex, req);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    return editor.makeState();
  }

  @Nonnull
  private State initialStateWithoutBatteryDistance(Vertex startVertex, String network, boolean arriveBy) {
    State state = initialStateWithoutBackState(startVertex, network, arriveBy);
    var edge1 = streetEdge(V1, V2);

    StateEditor editor = new StateEditor(state, edge1);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    return editor.makeState();

  }

  @Nonnull
  private State initialStateWithBatteryDistance(Vertex startVertex, String network, boolean arriveBy) {
    State state = initialStateWithoutBackState(startVertex, network, arriveBy);
    var edge1 = streetEdge(V1, V2);
    state.currentRangeMeters = 1000;
    state.traversedBatteryMeters =250;

    StateEditor editor = new StateEditor(state, edge1);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    return editor.makeState();
  }
}
