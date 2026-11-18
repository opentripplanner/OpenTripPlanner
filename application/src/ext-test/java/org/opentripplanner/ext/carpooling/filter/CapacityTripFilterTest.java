package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithCapacity;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolingRequestTestData;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;

class CapacityTripFilterTest {

  private final CapacityTripFilter filter = new CapacityTripFilter();
  private final SnappedPassenger passenger = new SnappedPassenger(
    CarpoolingRequestTestData.departAfterDirect(Instant.now()),
    null,
    null
  );

  @Test
  void aCarWithAFreeSeatOnSomeLegIsACandidate() {
    // Room for two: full on the first leg, one seat free on the second.
    var trip = createTripWithCapacity(
      2,
      List.of(
        createStopAt(2, OSLO_SOUTH),
        createStopAt(1, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );

    assertTrue(
      filter.isCandidateTrip(RoutableCarpoolTripTestData.withDummyVertices(trip), passenger)
    );
  }

  @Test
  void aCarFullOnEveryLegIsNot() {
    var trip = createTripWithCapacity(
      2,
      List.of(
        createStopAt(2, OSLO_SOUTH),
        createStopAt(2, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );

    assertFalse(
      filter.isCandidateTrip(RoutableCarpoolTripTestData.withDummyVertices(trip), passenger)
    );
  }
}
