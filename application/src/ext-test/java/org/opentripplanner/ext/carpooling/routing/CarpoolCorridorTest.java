package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CarpoolCorridorTest {

  @Test
  void oneLimitPerLegIsRequired() {
    assertThrows(IllegalArgumentException.class, () ->
      new CarpoolCorridor(List.of(Duration.ofMinutes(15)), List.of(), List.of())
    );
  }
}
