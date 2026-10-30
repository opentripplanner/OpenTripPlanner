package org.opentripplanner.ext.carpooling.model;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolTestCoordinates;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;

class CarpoolTripSameAsTest {

  private final CarpoolTrip trip = CarpoolTripTestData.createSimpleTrip(
    CarpoolTestCoordinates.OSLO_CENTER,
    CarpoolTestCoordinates.OSLO_EAST
  );

  @Test
  void twoEqualTripsAreTheSame() {
    assertTrue(tripWithFixedValues().sameAs(tripWithFixedValues()));
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

  private static CarpoolTrip tripWithFixedValues() {
    var start = ZonedDateTime.parse("2026-01-01T08:00+01:00");
    return new CarpoolTripBuilder(new FeedScopedId("TEST", "trip"))
      .withStops(
        List.of(
          CarpoolStop.of(new FeedScopedId("TEST", "origin"))
            .withCoordinate(CarpoolTestCoordinates.OSLO_CENTER)
            .build(),
          CarpoolStop.of(new FeedScopedId("TEST", "destination"))
            .withCoordinate(CarpoolTestCoordinates.OSLO_EAST)
            .build()
        )
      )
      .withStartTime(start)
      .withEndTime(start.plusHours(1))
      .build();
  }
}
