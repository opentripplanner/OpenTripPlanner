package org.opentripplanner.framework.snapshot.persistence.repository.timetable;

public class TimetableConfig {

  public static ImmutableTimetableRepo provideTimetableRepo() {
    return new ImmutableTimetableRepo("abc");
  }
}
