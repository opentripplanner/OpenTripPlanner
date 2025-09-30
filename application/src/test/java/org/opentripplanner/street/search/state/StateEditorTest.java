package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public class StateEditorTest {

  static Vertex vertex = StreetModelForTest.intersectionVertex(1, 1);

  @Test
  public final void testIncrementTimeInMilliseconds() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementTimeInMilliseconds(999999999);

    assertEquals(999999999, stateEditor.child.getTimeMilliseconds());
  }

  @Test
  public final void testWeightIncrement() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(10);

    assertNotNull(stateEditor.makeState());
  }

  @Test
  public final void testNanWeightIncrement() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NaN);

    assertNull(stateEditor.makeState());
  }

  @Test
  public final void testInfinityWeightIncrement() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NEGATIVE_INFINITY);

    assertNull(stateEditor.makeState(), "Infinity weight increment");
  }

  @Nested
  class GeofencingZones {

    StreetVertex v1 = StreetModelForTest.intersectionVertex(0, 0);
    StreetVertex v2 = StreetModelForTest.intersectionVertex(1, 1);
    StreetEdge edge1 = StreetModelForTest.streetEdge(v1, v2);

    @Test
    void forwardEnterZone() {
      var editor = new StateEditor(
        v1,
        StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build()
      );
      editor.enterNoRentalDropOffArea();
      var state = editor.makeState();
      assertTrue(state.isInsideNoRentalDropOffArea());

      var secondEditor = state.edit(edge1);
      secondEditor.enterNoRentalDropOffArea();
      var secondState = secondEditor.makeState();
      assertTrue(secondState.isInsideNoRentalDropOffArea());
    }

    @Test
    void leaveZone() {
      var editor = new StateEditor(
        v1,
        StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build()
      );
      editor.enterNoRentalDropOffArea();
      var state = editor.makeState();
      assertTrue(state.isInsideNoRentalDropOffArea());

      var secondEditor = state.edit(edge1);
      secondEditor.leaveNoRentalDropOffArea();
      var secondState = secondEditor.makeState();
      assertFalse(secondState.isInsideNoRentalDropOffArea());
    }
  }

  @Nested
  class ParkAndRide {

    StreetVertex v1 = StreetModelForTest.intersectionVertex(0, 0);
    StreetVertex v2 = StreetModelForTest.intersectionVertex(1, 1);
    StreetEdge edge1 = StreetModelForTest.streetEdge(v1, v2);

    @Test
    void resetNoThroughAfterParkAndRide() {
      var editor = new StateEditor(v1, StreetSearchRequest.of().withMode(StreetMode.CAR).build());
      editor.setEnteredNoThroughTrafficArea();
      var state = editor.makeState();
      assertTrue(state.hasEnteredNoThruTrafficArea());

      var secondEditor = state.edit(edge1);
      secondEditor.setVehicleParked(true, TraverseMode.WALK);
      var secondState = secondEditor.makeState();
      assertFalse(secondState.hasEnteredNoThruTrafficArea());
    }
  }
}
