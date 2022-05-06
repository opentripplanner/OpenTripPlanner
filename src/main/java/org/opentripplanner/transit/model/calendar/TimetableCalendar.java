package org.opentripplanner.transit.model.calendar;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.trip.Timetable;
import org.opentripplanner.transit.model.trip.TripOnDate;

/**
 * Collection of timetables witch provide access to the next/current/prev timetable,
 * initially pointing to the timetable of the current day.
 */
public class TimetableCalendar implements RaptorTripScheduleSearch<TripOnDate> {

  /**
   * If there is no schedule on a given day we need to check the prev/next days.
   * The limit is set to 7 days. For the search to fail the following must be through:
   * The trip we search for run for more than 7 days, and there are no departures the
   * last 7 days from the trip origin. Note! that we might want to board at one of the last
   * stops(trip arriving after 7 days).
   */
  private static final int MAX_NUM_OF_DAYS_TO_SEARCH = 7;

  private final int patternIndex;
  private final ServiceCalendar calendar;

  private int currentDay;

  public TimetableCalendar(ServiceCalendar calendar, int patternIndex, int currentDay) {
    this.calendar = calendar;
    this.patternIndex = patternIndex;
    this.currentDay = currentDay;
  }

  int timeOffset() {
    return calendar.timeOffsets(currentDay);
  }

  @Nullable
  Timetable prev() {
    while (currentDay > 0) {
      --currentDay;
      var c = current();
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  @Nullable
  Timetable current() {
    return calendar.serviceDay(currentDay).timetable(patternIndex);
  }

  @Nullable
  Timetable next() {
    int limit = Math.max(currentDay + MAX_NUM_OF_DAYS_TO_SEARCH, calendar.numberOfDays());
    while (currentDay < limit) {
      ++currentDay;
      var c = current();
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    // TODO RTM
    return null;
  }
}
