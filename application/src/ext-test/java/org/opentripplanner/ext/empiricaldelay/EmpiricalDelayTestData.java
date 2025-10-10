package org.opentripplanner.ext.empiricaldelay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.ext.empiricaldelay.model.calendar.EmpiricalDelayCalendar;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class EmpiricalDelayTestData {

  static final String FEED_ID = "F";
  static final String EVERYDAY = "EVERYDAY";
  static final LocalDate START = LocalDate.of(2025, 1, 1);
  static final LocalDate END = LocalDate.of(2025, 12, 31);
  static final LocalDate DATE_INSIDE = LocalDate.of(2025, 5, 1);
  static final LocalDate DATE_OUTSIDE = LocalDate.of(2026, 1, 1);
  static final FeedScopedId TRIP_ID = new FeedScopedId(FEED_ID, "Trip-A");
  static final FeedScopedId UNKNOWN_TRIP_ID = new FeedScopedId(FEED_ID, "Trip-B");
  static final EmpiricalDelayCalendar SERVICE_CALENDAR = EmpiricalDelayCalendar.of()
    .with(EVERYDAY, Arrays.asList(DayOfWeek.values()), START, END)
    .build();
  static final EmpiricalDelay DELAY_STOP_1 = new EmpiricalDelay(12, 20);
  static final EmpiricalDelay DELAY_STOP_2 = new EmpiricalDelay(19, 210);
  static final EmpiricalDelay DELAY_STOP_3 = new EmpiricalDelay(13, 38);
}
