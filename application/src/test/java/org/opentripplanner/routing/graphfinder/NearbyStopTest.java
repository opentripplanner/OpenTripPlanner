package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class NearbyStopTest {

  private static TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();

  // TODO Add tests for all public methods in NearbyStop here

  @Test
  void testIsBetter() {
    // We only test the distance here, since the compareTo method used should have a more complete
    // unit-test including tests on state weight.
    var a = new NearbyStop(MODEL.stop("A").build(), 20.0, List.of(), List.of());
    var b = new NearbyStop(MODEL.stop("A").build(), 30.0, List.of(), List.of());

    assertTrue(a.isBetter(b));
    assertFalse(b.isBetter(a));

    var sameDistance = new NearbyStop(MODEL.stop("A").build(), 20.0, List.of(), List.of());
    assertFalse(a.isBetter(sameDistance));
    assertFalse(sameDistance.isBetter(a));
  }
}
