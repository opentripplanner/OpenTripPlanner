package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleSearch;

/**
 * Searches for a concrete trip time for a frequency based pattern. The {@link FrequencyEntry}s are
 * scanned to find the earliest possible departure time.
 */
public final class TripFrequencyAlightSearch<T extends RaptorTripSchedule> implements RaptorTripScheduleSearch<T> {

    private final TripPatternForDates timeTable;

    public TripFrequencyAlightSearch(RaptorTimeTable<T> timeTable) {
        this.timeTable = (TripPatternForDates) timeTable;
    }

    @Override
    public RaptorTripScheduleBoardOrAlightEvent<T> search(
            int earliestBoardTime,
            int stopPositionInPattern,
            int tripIndexLimit
    ) {
        IntIterator indexIterator = timeTable.tripPatternForDatesIndexIterator(false);
        while (indexIterator.hasNext()) {
            int i = indexIterator.next();
            var pattern = timeTable.tripPatternForDate(i);
            int offset = timeTable.tripPatternForDateOffsets(i);

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
                            timeTable,
                            tripTimes,
                            pattern.getTripPattern().getPattern(),
                            stopPositionInPattern,
                            arrivalTime + headway,
                            headway,
                            offset,
                            pattern.getLocalDate()
                    );
                }
            }
        }
        return null;
    }
}