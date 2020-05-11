package org.opentripplanner.transit.raptor.speed_test.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.util.TimeUtils;

public class AccessEgressLeg implements RaptorTransfer {
    private final int stop, durationInSeconds;

    public AccessEgressLeg(int stop, int durationInSeconds) {
        this.stop = stop;
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    public int stop() {
        return stop;
    }

    @Override
    public int earliestDepartureTime(int requestedDepartureTime) {
        return requestedDepartureTime;
    }

    @Override
    public int latestArrivalTime(int requestedArrivalTime) {
        return requestedArrivalTime;
    }

    @Override
    public int durationInSeconds() {
        return durationInSeconds;
    }


    @Override
    public String toString() {
        return TimeUtils.timeToStrCompact(durationInSeconds) + " " + stop;
    }
}
