package org.opentripplanner.transit.model.plan;

import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.transit.model.calendar.TripScheduleSearchOnDays;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.trip.TripOnDate;
import org.opentripplanner.transit.model.trip.timetable.DefaultTimetable;

public record RaptorRouteAdaptor(
  RaptorTripPattern routingPattern,
  TripScheduleSearchOnDays timetables
)
  implements
    RaptorRoute<TripOnDate>, RaptorTimeTable<TripOnDate>, RaptorTripScheduleSearch<TripOnDate> {
  /* implement RaptorRoute */

  @Override
  public RaptorTimeTable<TripOnDate> timetable() {
    return this;
  }

  @Override
  public RaptorTripPattern pattern() {
    return routingPattern;
  }

  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestBoardTime,
    int stopPositionInPattern
  ) {
    return null;
  }

  /* implement RaptorTimeTable */

  @Override
  public TripOnDate getTripSchedule(int index) {
    // TODO RTM
    return null;
  }

  @Override
  public int numberOfTripSchedules() {
    // TODO RTM -
    return 0;
  }

  @Override
  public RaptorTripScheduleSearch<TripOnDate> tripSearch(SearchDirection direction) {
    // TODO RTM

    return new RaptorTripScheduleSearch<TripOnDate>() {
      @Override
      public RaptorBoardOrAlightEvent<TripOnDate> search(
        int earliestBoardTime,
        int stopPositionInPattern,
        int tripIndexLimit
      ) {
        return new RaptorBoardOrAlightEvent<TripOnDate>() {
          @Override
          public int tripIndex() {
            return 0;
          }

          @Override
          public TripOnDate trip() {
            DefaultTimetable timetable = DefaultTimetable.create(
              new int[][] { { 60, 180, 300 } },
              new int[][] { { 60, 180, 300 } },
              new Deduplicator()
            );
            return new TripOnDate(0, timetable);
          }

          @Override
          public int stopPositionInPattern() {
            return 0;
          }

          @Override
          public int time() {
            return 0;
          }

          @Override
          public int earliestBoardTime() {
            return 0;
          }

          @Nonnull
          @Override
          public RaptorTransferConstraint transferConstraint() {
            return RaptorTransferConstraint.REGULAR_TRANSFER;
          }

          @Override
          public boolean empty() {
            return false;
          }
        };
      }
    };
  }

  /* implement RaptorTripScheduleSearch<> */

  @Override
  public RaptorBoardOrAlightEvent<TripOnDate> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    return null;
  }
}
