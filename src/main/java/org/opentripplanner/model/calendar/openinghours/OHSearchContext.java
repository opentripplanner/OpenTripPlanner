package org.opentripplanner.model.calendar.openinghours;

import java.time.Duration;
import java.time.Instant;

public class OHSearchContext {

  private final Instant searchStartTime;
  private final Instant searchEndTime;

  public OHSearchContext(Instant searchStartTime, Duration maxJourneyDuration) {
    if (maxJourneyDuration.isNegative()) {
      this.searchStartTime = searchStartTime.plus(maxJourneyDuration);
      this.searchEndTime = searchStartTime;
    } else {
      this.searchStartTime = searchStartTime;
      this.searchEndTime = searchStartTime.plus(maxJourneyDuration);
    }
  }

  public boolean isOpen(OHCalendar calendar, long timeEpochSecond) {
    return calendar.isOpen(timeEpochSecond);
  }

  public boolean canEnter(OHCalendar calendar, long epochSecond) {
    return isOpen(calendar, epochSecond);
  }

  public boolean canExit(OHCalendar calendar, long epochSecond) {
    return isOpen(calendar, epochSecond);
  }
}
