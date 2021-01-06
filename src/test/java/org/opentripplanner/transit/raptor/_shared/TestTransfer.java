package org.opentripplanner.transit.raptor._shared;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class TestTransfer implements RaptorTransfer {
    private final int stop;
    private final int durationInSeconds;

    public TestTransfer(int stop, int durationInSeconds) {
        this.stop = stop;
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    public int stop() {
        return stop;
    }

    @Override
    public int durationInSeconds() {
        return durationInSeconds;
    }
}
