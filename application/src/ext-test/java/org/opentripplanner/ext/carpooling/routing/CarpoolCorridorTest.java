package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class CarpoolCorridorTest {

  @Test
  void oneLimitAndOneEnvelopePerLegAreRequired() {
    assertThrows(IllegalArgumentException.class, () ->
      new CarpoolCorridor(
        List.of(Duration.ofMinutes(15)),
        List.of(),
        List.of(),
        List.of(new Envelope(10.0, 11.0, 63.0, 64.0))
      )
    );
  }
}
