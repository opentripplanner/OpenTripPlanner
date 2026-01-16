package org.opentripplanner.framework.snapshot;

public class TimetableRepo {

  private final String tripId;

  public TimetableRepo(String tripId) {
    this.tripId = tripId;
  }

  public TimetableRepo updateTrip(String tripId) {
    return new TimetableRepo(tripId);
  }

  public String getTripId() {
    return tripId;
  }
}
