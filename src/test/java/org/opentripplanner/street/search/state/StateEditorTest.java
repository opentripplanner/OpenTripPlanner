package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public class StateEditorTest {

  @Test
  public final void testIncrementTimeInSeconds() {
    StateEditor stateEditor = new StateEditor(null, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementTimeInSeconds(999999999);

    assertEquals(999999999, stateEditor.child.getTimeSeconds());
  }

  @Test
  public final void testWeightIncrement() {
    StateEditor stateEditor = new StateEditor(null, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(10);

    assertNotNull(stateEditor.makeState());
  }

  @Test
  public final void testNanWeightIncrement() {
    StateEditor stateEditor = new StateEditor(null, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NaN);

    assertNull(stateEditor.makeState());
  }

  @Test
  public final void testInfinityWeightIncrement() {
    StateEditor stateEditor = new StateEditor(null, StreetSearchRequest.of().build());

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NEGATIVE_INFINITY);

    assertNull(stateEditor.makeState(), "Infinity weight increment");
  }
}
