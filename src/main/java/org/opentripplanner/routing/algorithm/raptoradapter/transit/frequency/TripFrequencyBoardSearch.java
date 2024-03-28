package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Searches for a concrete trip time for a frequency based pattern. The {@link FrequencyEntry}s are
 * scanned to find the latest possible alighting time.
 */
public final class TripFrequencyBoardSearch<T extends DefaultTripSchedule>
  implements RaptorTripScheduleSearch<T> {

  private final TripPatternForDates patternForDates;

  public TripFrequencyBoardSearch(TripPatternForDates patternForDates) {
    this.patternForDates = patternForDates;
  }

  @Override
  public RaptorBoardOrAlightEvent<T> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    IntIterator indexIterator = patternForDates.tripPatternForDatesIndexIterator(true);
    while (indexIterator.hasNext()) {
      int i = indexIterator.next();
      var pattern = patternForDates.tripPatternForDate(i);
      int offset = patternForDates.tripPatternForDateOffsets(i);

      for (var frequency : pattern.getFrequencies()) {
        var departureTime = frequency.nextDepartureTime(
          stopPositionInPattern,
          earliestBoardTime - offset
        );
        if (departureTime != -1) {
          int headway = frequency.exactTimes ? 0 : frequency.headway;
          TripTimes tripTimes = frequency.materialize(
            stopPositionInPattern,
            departureTime - headway,
            true
          );

          return new FrequencyBoardingEvent<>(
            patternForDates,
            tripTimes,
            stopPositionInPattern,
            earliestBoardTime,
            departureTime - headway,
            headway,
            offset,
            pattern.getServiceDate()
          );
        }
      }
    }
    return RaptorBoardOrAlightEvent.empty(earliestBoardTime);
  }
}
