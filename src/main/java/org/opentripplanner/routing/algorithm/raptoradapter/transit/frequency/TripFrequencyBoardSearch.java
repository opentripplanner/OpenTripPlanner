package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleSearch;

/**
 * Searches for a concrete trip time for a frequency based pattern. The {@link FrequencyEntry}s are
 * scanned to find the latest possible alighting time.
 */
public final class TripFrequencyBoardSearch<T extends RaptorTripSchedule> implements RaptorTripScheduleSearch<T> {

    private final TripPatternForDates patternForDates;

    public TripFrequencyBoardSearch(TripPatternForDates patternForDates) {
        this.patternForDates = patternForDates;
    }

    @Override
    public RaptorTripScheduleBoardOrAlightEvent<T> search(
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
                var departureTime = frequency.nextDepartureTime(stopPositionInPattern, earliestBoardTime - offset);
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
                            pattern.getTripPattern().getPattern(),
                            stopPositionInPattern,
                            departureTime - headway,
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