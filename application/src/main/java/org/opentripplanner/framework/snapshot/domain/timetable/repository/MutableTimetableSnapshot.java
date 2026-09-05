package org.opentripplanner.framework.snapshot.domain.timetable.repository;

import java.util.ArrayList;
import java.util.List;

public class MutableTimetableSnapshot {

  private final List<String> trips;

  MutableTimetableSnapshot(List<String> trips) {
    this.trips = new ArrayList<>(trips);
  }

  public void addTrip(String tripId) {
    trips.add(tripId);
  }

  public void cancelTrip(String tripId) {
    trips.remove(tripId);
  }

  public List<String> getTrips() {
    return trips;
  }
}
