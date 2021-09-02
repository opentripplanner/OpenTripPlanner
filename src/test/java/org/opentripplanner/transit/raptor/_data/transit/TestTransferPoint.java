package org.opentripplanner.transit.raptor._data.transit;

import org.opentripplanner.model.transfer.TransferPoint;

public class TestTransferPoint implements TransferPoint {
    private final int stop;
    private final TestTripSchedule schedule;

    public TestTransferPoint(
            int stop,
            TestTripSchedule schedule
    ) {
        this.stop = stop;
        this.schedule = schedule;
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
}
