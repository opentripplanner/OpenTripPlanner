package org.opentripplanner.transit.raptor._data.transit;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple implementation for {@link RaptorTransfer} for use in unit-tests.
 */
public class TestTransfer implements RaptorTransfer {

    public static final int DEFAULT_NUMBER_OF_LEGS = 0;
    public static final boolean STOP_REACHED_ON_BOARD = true;
    public static final boolean STOP_REACHED_ON_FOOT = false;
    private final int stop;
    private final int durationInSeconds;
    private final int numberOfRides;
    private final boolean stopReachedOnBoard;

    private TestTransfer(
        int stop,
        int durationInSeconds,
        int numberOfRides,
        boolean stopReachedOnBoard
    ) {
        this.stop = stop;
        this.durationInSeconds = durationInSeconds;
        this.numberOfRides = numberOfRides;
        this.stopReachedOnBoard = stopReachedOnBoard;
    }

    /** Only use this to override this class, use factory methods to create instances. */
    protected TestTransfer(int stop, int durationInSeconds) {
        this(stop, durationInSeconds, DEFAULT_NUMBER_OF_LEGS, STOP_REACHED_ON_FOOT);
    }

    public static TestTransfer walk(int stop, int durationInSeconds) {
        return new TestTransfer(stop, durationInSeconds, DEFAULT_NUMBER_OF_LEGS, STOP_REACHED_ON_FOOT);
    }

    /** Create a new flex access and arrive stop onBoard with 1 ride/extra transfer. */
    public static TestTransfer flex(int stop, int durationInSeconds) {
        return flex(stop, durationInSeconds, 1);
    }

    /** Create a new flex access and arrive stop onBoard. */
    public static TestTransfer flex(int stop, int durationInSeconds, int nRides) {
        assert nRides > DEFAULT_NUMBER_OF_LEGS;
        return new TestTransfer(stop, durationInSeconds, nRides, STOP_REACHED_ON_BOARD);
    }

    /** Create a flex access arriving at given stop by walking with 1 ride/extra transfer. */
    public static TestTransfer flexAndWalk(int stop, int durationInSeconds) {
        return flexAndWalk(stop, durationInSeconds, 1);
    }

    /** Create a flex access arriving at given stop by walking. */
    public static TestTransfer flexAndWalk(int stop, int durationInSeconds, int nRides) {
        assert nRides > DEFAULT_NUMBER_OF_LEGS;
        return new TestTransfer(stop, durationInSeconds, nRides, STOP_REACHED_ON_FOOT);
    }

    public static Collection<RaptorTransfer> transfers(int ... stopTimes) {
        List<RaptorTransfer> legs = new ArrayList<>();
        for (int i = 0; i < stopTimes.length; i+=2) {
            legs.add(walk(stopTimes[i], stopTimes[i+1]));
        }
        return legs;
    }

    @Override
    public int stop() {
        return stop;
    }

    @Override
    public int durationInSeconds() {
        return durationInSeconds;
    }

    @Override
    public int numberOfRides() {
        return numberOfRides;
    }

    @Override
    public boolean stopReachedOnBoard() {
        return stopReachedOnBoard;
    }

    @Override
    public String toString() {
        return asString();
    }
}
