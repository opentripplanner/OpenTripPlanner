package org.opentripplanner.framework.snapshot.persistence.repository.timetable;

import jakarta.ws.rs.NotSupportedException;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;

public class ImmutableTimetableRepo implements TimetableRepo {

  private final String tripId;

  public ImmutableTimetableRepo(String tripId) {
    this.tripId = tripId;
  }

  public String getTripId() {
    return tripId;
  }

  @Override
  public void setTripId(String tripId) {
    throw new NotSupportedException("read only access, no writes allowed");
  }
}
