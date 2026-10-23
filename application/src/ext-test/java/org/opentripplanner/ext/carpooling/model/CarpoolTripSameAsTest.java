package org.opentripplanner.ext.carpooling.model;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTestCoordinates;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;

class CarpoolTripSameAsTest {

  private final CarpoolTrip trip = CarpoolTripTestData.createSimpleTrip(
    CarpoolTestCoordinates.OSLO_CENTER,
    CarpoolTestCoordinates.OSLO_EAST
  );

  @Test
  void aCopyIsTheSame() {
    assertTrue(trip.sameAs(new CarpoolTripBuilder(trip).build()));
  }

  @Test
  void aChangedCapacityIsNotTheSame() {
    var moreSeats = new CarpoolTripBuilder(trip)
      .withTotalCapacity(trip.totalCapacity() + 1)
      .build();
    assertFalse(trip.sameAs(moreSeats));
  }

  @Test
  void aChangedStopIsNotTheSameEvenThoughTheStopIdsAre() {
    var stops = new ArrayList<>(trip.stops());
    var last = stops.getLast();
    stops.set(
      stops.size() - 1,
      last
        .copy()
        .withDeviationBudget(last.getDeviationBudget().plus(Duration.ofMinutes(5)))
        .build()
    );
    var changed = new CarpoolTripBuilder(trip).withStops(stops).build();
    assertWithMessage("entity equality is by id").that(changed.stops()).isEqualTo(trip.stops());
    assertFalse(trip.sameAs(changed));
  }
}
