package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public class StateEditorTest {

  static Vertex vertex = StreetModelFactory.intersectionVertex(1, 1);

  @Test
  public final void testIncrementTimeInMilliseconds() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementTimeInMilliseconds(999999999);

    assertEquals(999999999, stateEditor.makeState().getTimeMilliseconds());
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
    assertThrows(IllegalArgumentException.class, () -> stateEditor.incrementWeight(Double.NaN));
  }

  @Test
  public final void testInfinityWeightIncrement() {
    StateEditor stateEditor = new StateEditor(vertex, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    assertThrows(IllegalArgumentException.class, () ->
      stateEditor.incrementWeight(Double.NEGATIVE_INFINITY)
    );
  }

  @Nested
  class ParkAndRide {

    StreetVertex v1 = StreetModelFactory.intersectionVertex(0, 0);
    StreetVertex v2 = StreetModelFactory.intersectionVertex(1, 1);
    StreetEdge edge1 = StreetModelFactory.streetEdge(v1, v2);

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
