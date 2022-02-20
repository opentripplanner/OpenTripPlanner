package org.opentripplanner.transit.raptor.rangeraptor.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

final class FrequencyAlightEvent<T extends RaptorTripSchedule> extends FrequencyBoardOrAlightEvent<T> {

    public FrequencyAlightEvent(
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

    @Override
    public int arrival(int stopPosInPattern) {
        return tripTimes.getArrivalTime(stopPosInPattern) + offset;
    }

    // Remove headway here to report an early enough departure time for the raptor search
    @Override
    public int departure(int stopPosInPattern) {
        return tripTimes.getDepartureTime(stopPosInPattern) - headway + offset;
    }

}
