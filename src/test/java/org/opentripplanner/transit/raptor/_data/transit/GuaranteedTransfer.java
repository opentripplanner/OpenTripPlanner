package org.opentripplanner.transit.raptor._data.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

class GuaranteedTransfer implements RaptorTripScheduleBoardOrAlightEvent<TestTripSchedule> {

    private final TestTripSchedule sourceTrip;
    private final int sourceStopPos;
    private final TestTripSchedule targetTrip;
    private final int targetTripIndex;
    private final int targetStopPos;
    private final int targetTime;

    GuaranteedTransfer(
            TestTripSchedule sourceTrip,
            int sourceStopPos,
            TestTripSchedule targetTrip,
            int targetTripIndex,
            int targetStopPos,
            int targetTime
    ) {
        this.sourceTrip = sourceTrip;
        this.sourceStopPos = sourceStopPos;
        this.targetTrip = targetTrip;
        this.targetTripIndex = targetTripIndex;
        this.targetStopPos = targetStopPos;
        this.targetTime = targetTime;
    }

    @Override
    public int getTripIndex() {
        return targetTripIndex;
    }

    @Override
    public TestTripSchedule getTrip() {
        return targetTrip;
    }

    @Override
    public int getStopPositionInPattern() {
        return targetStopPos;
    }

    @Override
    public int getTime() {
        return targetTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(GuaranteedTransfer.class)
                .addObj("sourceTrip", sourceTrip)
                .addNum("sourceStopPos", sourceStopPos)
                .addObj("targetTrip", targetTrip)
                .addNum("targetTripIndex", targetTripIndex)
                .addNum("targetStopPos", targetStopPos)
                .addServiceTime("targetTime", targetTime)
                .toString();
    }

    TestTripSchedule getSourceTrip() {
        return sourceTrip;
    }

    int getSourceStopPos() {
        return sourceStopPos;
    }
}
