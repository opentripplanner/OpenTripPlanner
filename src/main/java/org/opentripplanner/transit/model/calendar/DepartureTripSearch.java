package org.opentripplanner.transit.model.calendar;

import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.trip.Timetable;
import org.opentripplanner.transit.model.trip.TripOnDate;

class DepartureTripSearch
  implements RaptorTripScheduleSearch<TripOnDate>, RaptorBoardOrAlightEvent<TripOnDate> {

  private final TimetableCalendar timetables;

  private int timeOffset;
  private int tripIndexOffset;
  private int tripIndexDay;
  private int stopPositionInPattern;

  public DepartureTripSearch(TimetableCalendar timetables) {
    this.timetables = timetables;
  }

  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestDepartureTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    this.stopPositionInPattern = stopPositionInPattern;
    int edt = earliestDepartureTime - timetables.timeOffset();
    boolean gotoPrev = false;
    boolean gotoNext = false;

    // TODO RTM - If the timetable is initialized correct this should not happen,
    //          - but if it happend the while loop can be used to skip to the correct day
    while (edt < 0) {
      throw new IllegalStateException("Unexpected");
      //if(timetables.next() == null) {
      //  // EDT is after last trip in timetable calendar
      //  return null;
      //}
      //edt = earliestDepartureTime - timetables.timeOffset();
    }

    while (timetables.current() != null) {
      var tt = timetables.current();

      int tripIndex = tt.findTripIndexBoardingAfter(stopPositionInPattern, edt);

      if (tripIndex == Timetable.NEXT_TIME_TABLE_INDEX) {
        if (gotoPrev) {
          return this;
        }
        if (timetables.next() == null) {
          return null;
        }
        gotoNext = true;
      } else if (tripIndex == Timetable.PREV_TIME_TABLE_INDEX) {
        if (timetables.prev() == null) {
          return null;
        }
      } else {
        // TODO RTM - Return trip with tripIndex here
        return null;
      }
      edt = earliestDepartureTime - timetables.timeOffset();
    }
    return null;
  }

  @Override
  public int tripIndex() {
    return tripIndexOffset + tripIndexDay;
  }

  @Override
  public TripOnDate trip() {
    return null;
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public int time() {
    //noinspection ConstantConditions
    return timetables.current().boardTime(tripIndexDay, stopPositionInPattern);
  }

  @Override
  public int earliestBoardTime() {
    // TODO RTM
    return 0;
  }

  @Override
  public RaptorTransferConstraint transferConstraint() {
    // TODO RTM
    return null;
  }

  @Override
  public boolean empty() {
    return false;
  }
}
