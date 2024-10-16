package org.opentripplanner.transit.model.network.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.timetable.Trip;

class TripAdapterTest {

  private final Trip trip = TimetableRepositoryForTest.trip("Trip").build();

  private final TripAdapter subject = new TripAdapter(trip);

  @Test
  void mode() {
    assertEquals(trip.getMode(), subject.mode());
  }

  @Test
  void subMode() {
    assertEquals(trip.getNetexSubMode().name(), subject.subMode());
  }

  @Test
  void agencyId() {
    assertEquals(trip.getRoute().getAgency().getId(), subject.agencyId());
  }

  @Test
  void routeId() {
    assertEquals(trip.getRoute().getId(), subject.routeId());
  }
}
