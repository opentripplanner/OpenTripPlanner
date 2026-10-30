package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

public class RaptorTransitDataTestFactory {

  public static RaptorTransitData empty() {
    return new RaptorTransitData(
      Map.of(LocalDate.of(2026, 5, 27), Collections.emptyList()),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
}
