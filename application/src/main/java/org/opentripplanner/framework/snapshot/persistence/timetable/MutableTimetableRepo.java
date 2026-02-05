package org.opentripplanner.framework.snapshot.persistence.timetable;

import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;

public class MutableTimetableRepo implements TimetableRepo {

  private String tripId;

  private MutableTimetableRepo(String tripId) {
    this.tripId = tripId;
  }

  public static MutableTimetableRepo from(ImmutableTimetableRepo timetableRepo) {
    return new MutableTimetableRepo(timetableRepo.getTripId());
  }

  public ImmutableTimetableRepo freeze() {
    return new ImmutableTimetableRepo(tripId);
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public String getTripId() {
    return tripId;
  }
}
