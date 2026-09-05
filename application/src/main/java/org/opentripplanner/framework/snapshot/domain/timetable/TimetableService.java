package org.opentripplanner.framework.snapshot.domain.timetable;

import java.util.List;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.ReadOnlyTimetableSnapshot;

public class TimetableService {

  private final ReadOnlyTimetableSnapshot timetableSnapshot;

  public TimetableService(ReadOnlyTimetableSnapshot timetableSnapshot) {
    this.timetableSnapshot = timetableSnapshot;
  }

  public List<String> getTrips() {
    return timetableSnapshot.getTrips();
  }
}
