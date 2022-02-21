package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

abstract class FrequencyBoardOrAlightEvent<T extends RaptorTripSchedule>
        implements RaptorTripScheduleBoardOrAlightEvent<T>, TripSchedule {

    protected final RaptorTripPattern raptorTripPattern;
    protected final TripTimes tripTimes;
    protected final TripPattern pattern;
    protected final int stopPositionInPattern;
    protected final int departureTime;
    protected final int offset;
    protected final int headway;
    protected final LocalDate serviceDate;

    public FrequencyBoardOrAlightEvent(
            RaptorTripPattern raptorTripPattern,
            TripTimes tripTimes,
            TripPattern pattern,
            int stopPositionInPattern,
            int departureTime,
            int offset,
            int headway,
            LocalDate serviceDate
    ) {
        this.raptorTripPattern = raptorTripPattern;
        this.tripTimes = tripTimes;
        this.pattern = pattern;
        this.stopPositionInPattern = stopPositionInPattern;
        this.departureTime = departureTime;
        this.offset = offset;
        this.headway = headway;
        this.serviceDate = serviceDate;
    }

    /* RaptorTripScheduleBoardOrAlightEvent implementation */

    @Override
    public int getTripIndex() {
        return tripTimes.getDepartureTime(0) + offset;
    }

    @Override
    public T getTrip() {
        return (T) this;
    }

    @Override
    public int getStopPositionInPattern() {
        return stopPositionInPattern;
    }

    @Override
    public int getTime() {
        return departureTime + offset;
    }

    @Override
    public RaptorTransferConstraint getTransferConstraint() {
        return RaptorTransferConstraint.REGULAR_TRANSFER;
    }

    /* RaptorTripSchedule implementation */

    @Override
    public int tripSortIndex() {
        return tripTimes.getDepartureTime(0) + offset;
    }

    @Override
    public abstract int arrival(int stopPosInPattern);

    @Override
    public abstract int departure(int stopPosInPattern);

    @Override
    public RaptorTripPattern pattern() {
        return raptorTripPattern;
    }

    @Override
    public int transitReluctanceFactorIndex() {
        return pattern.getMode().ordinal();
    }


    /* TripSchedule implementation */

    @Override
    public TripTimes getOriginalTripTimes() {
        return tripTimes;
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return pattern;
    }

    @Override
    public LocalDate getServiceDate() {
        return serviceDate;
    }

    @Override
    public boolean isFrequencyBasedTrip() { return true; }

    @Override
    public int frequencyHeadwayInSeconds() {
        return headway;
    }
}
