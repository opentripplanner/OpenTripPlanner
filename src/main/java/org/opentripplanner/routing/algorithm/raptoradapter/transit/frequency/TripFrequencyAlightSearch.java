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
 * scanned to find the earliest possible departure time.
 */
public final class TripFrequencyAlightSearch<T extends DefaultTripSchedule>
  implements RaptorTripScheduleSearch<T> {

  private final TripPatternForDates patternForDates;

  public TripFrequencyAlightSearch(TripPatternForDates patternForDates) {
    this.patternForDates = patternForDates;
  }

  @Override
  public RaptorBoardOrAlightEvent<T> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    IntIterator indexIterator = patternForDates.tripPatternForDatesIndexIterator(false);
    while (indexIterator.hasNext()) {
      int i = indexIterator.next();
      var pattern = patternForDates.tripPatternForDate(i);
      int offset = patternForDates.tripPatternForDateOffsets(i);

      for (int j = pattern.getFrequencies().size() - 1; j >= 0; j--) {
        final FrequencyEntry frequency = pattern.getFrequencies().get(j);
        var arrivalTime = frequency.prevArrivalTime(
          stopPositionInPattern,
          earliestBoardTime - offset
        );
        if (arrivalTime != -1) {
          int headway = frequency.exactTimes ? 0 : frequency.headway;
          TripTimes tripTimes = frequency.materialize(
            stopPositionInPattern,
            arrivalTime + headway,
            false
          );

          return new FrequencyAlightEvent<>(
            patternForDates,
            tripTimes,
            stopPositionInPattern,
            earliestBoardTime,
            arrivalTime + headway,
            headway,
            offset,
            pattern.getServiceDate()
          );
        }
      }
    }
    return null;
  }
}
