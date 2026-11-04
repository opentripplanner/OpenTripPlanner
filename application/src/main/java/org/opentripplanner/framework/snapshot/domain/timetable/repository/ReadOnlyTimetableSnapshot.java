package org.opentripplanner.framework.snapshot.domain.timetable.repository;

import java.util.List;

public class ReadOnlyTimetableSnapshot {

  private final List<String> trips;

  public ReadOnlyTimetableSnapshot(List<String> trips) {
    this.trips = List.copyOf(trips);
  }

  public List<String> getTrips() {
    return trips;
  }
}
