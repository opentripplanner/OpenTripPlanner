package org.opentripplanner.transit.raptor._data.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.TransferPoint;

public class TestTransferPoint implements TransferPoint {
    private final int stop;
    private final TestTripSchedule schedule;
    private final boolean applyToAllTrips;

    public TestTransferPoint(
            int stop,
            TestTripSchedule schedule,
            boolean applyToAllTrips

    ) {
        this.stop = stop;
        this.schedule = schedule;
        this.applyToAllTrips = applyToAllTrips;
    }

    @Override
    public boolean appliesToAllTrips() {
        return applyToAllTrips;
    }

    public int getStopPosition() {
        return schedule.pattern().findStopPositionAfter(0, stop);
    }

    @Override
    public int getSpecificityRanking() {
        return 2;
    }

    public boolean matches(TestTripSchedule schedule, int stop) {
        return this.schedule == schedule && this.stop == stop;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of()
                .addNum("stop", stop)
                .addObj("trip", schedule.pattern().debugInfo())
                .toString();
    }
}
