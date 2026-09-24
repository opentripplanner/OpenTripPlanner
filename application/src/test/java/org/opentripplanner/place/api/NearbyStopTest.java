package org.opentripplanner.place.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import org.junit.jupiter.api.Test;

class NearbyStopTest {

  @Test
  void testIsBetter() {
    // We only test the distance here, since the compareTo method used should have a more complete
    // unit-test including tests on state weight.
    var a = new NearbyStop(id("A"), 20.0, List.of(), List.of());
    var b = new NearbyStop(id("B"), 30.0, List.of(), List.of());

    assertTrue(a.isBetter(b));
    assertFalse(b.isBetter(a));

    var sameDistance = new NearbyStop(id("A"), 20.0, List.of(), List.of());
    assertFalse(a.isBetter(sameDistance));
    assertFalse(sameDistance.isBetter(a));
  }
}
