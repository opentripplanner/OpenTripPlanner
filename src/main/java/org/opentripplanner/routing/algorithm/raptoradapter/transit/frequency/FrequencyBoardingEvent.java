package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Represents a result of a {@link TripFrequencyBoardSearch}, with materialized {@link TripTimes}
 */
final class FrequencyBoardingEvent<T extends RaptorTripSchedule> extends FrequencyBoardOrAlightEvent<T> {

    public FrequencyBoardingEvent(
            RaptorTripPattern raptorTripPattern,
            TripTimes tripTimes,
            TripPattern pattern,
            int stopPositionInPattern,
            int departureTime,
            int headway,
            int offset,
            LocalDate serviceDate

    ) {
        super(
                raptorTripPattern,
                tripTimes,
                pattern,
                stopPositionInPattern,
                departureTime,
                offset,
                headway,
                serviceDate
        );
    }

    // Add headway here to report a late enough time to account for uncertainty
    @Override
    public int arrival(int stopPosInPattern) {
        return tripTimes.getArrivalTime(stopPosInPattern) + headway + offset;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return tripTimes.getDepartureTime(stopPosInPattern) + offset;
    }
}
