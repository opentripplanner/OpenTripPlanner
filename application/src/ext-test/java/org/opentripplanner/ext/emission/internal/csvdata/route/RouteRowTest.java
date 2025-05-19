package org.opentripplanner.ext.emission.internal.csvdata.route;

import static junit.framework.TestCase.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Gram;

class RouteRowTest {

  @Test
  void calculatePassengerCo2PerMeter() {
    var subject = new RouteRow("R:1", 2000.0, 0.5);
    assertEquals(Gram.of(4.0), subject.calculatePassengerCo2PerMeter());
  }
}
