package org.opentripplanner.transit.raptor._data.transit;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple implementation for {@link RaptorTransfer} for use in unit-tests.
 */
public class TestTransfer implements RaptorTransfer {

    public static final int SECONDS_IN_DAY = 24 * 3600;
    public static final int DEFAULT_NUMBER_OF_LEGS = 0;
    public static final boolean STOP_REACHED_ON_BOARD = true;
    public static final boolean STOP_REACHED_ON_FOOT = false;
    private final int stop;
    private final int durationInSeconds;
    private final int numberOfRides;
    private final boolean stopReachedOnBoard;
    private final Integer opening;
    private final Integer closing;

    private TestTransfer(
        int stop,
        int durationInSeconds,
        int numberOfRides,
        boolean stopReachedOnBoard
    ) {
        this(stop, durationInSeconds, numberOfRides, stopReachedOnBoard, null, null);
    }

    private TestTransfer(
            int stop,
            int durationInSeconds,
            int numberOfRides,
            boolean stopReachedOnBoard,
            Integer opening,
            Integer closing
    ) {
        this.stop = stop;
        this.durationInSeconds = durationInSeconds;
        this.numberOfRides = numberOfRides;
        this.stopReachedOnBoard = stopReachedOnBoard;
        this.opening = opening;
        this.closing = closing;
    }

    /** Only use this to override this class, use factory methods to create instances. */
    protected TestTransfer(int stop, int durationInSeconds) {
        this(stop, durationInSeconds, DEFAULT_NUMBER_OF_LEGS, STOP_REACHED_ON_FOOT);
    }

    public static TestTransfer walk(int stop, int durationInSeconds) {
        return new TestTransfer(stop, durationInSeconds, DEFAULT_NUMBER_OF_LEGS, STOP_REACHED_ON_FOOT, null, null);
    }

    /**
     * Creates a walk transfer with time restrictions. opening and closing may be specified as seconds
     * since the start of "RAPTOR time" to limit the time periods that the access is traversable, which
     * is repeatead every 24 hours. This allows the access to only be traversable between for example
     * 08:00 and 16:00 every day.
     */
    public static TestTransfer walk(int stop, int durationInSeconds, int opening, int closing) {
        return new TestTransfer(stop, durationInSeconds, DEFAULT_NUMBER_OF_LEGS, STOP_REACHED_ON_FOOT, opening, closing);
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

    /** Set opening and closing hours and return a new object. */
    public TestTransfer openingHours(int opening, int closing) {
        return new TestTransfer(stop, durationInSeconds, numberOfRides, stopReachedOnBoard, opening, closing);
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
    public int earliestDepartureTime(int requestedDepartureTime) {
        if (opening == null || closing == null) {
            return requestedDepartureTime;
        }

        int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_DAY);
        int specificOpening = days * SECONDS_IN_DAY + opening;
        int specificClosing = days * SECONDS_IN_DAY + closing;
        if (requestedDepartureTime < specificOpening) {
            return specificOpening;
        } else if (requestedDepartureTime > specificClosing) {
            // return the opening time for the next day
            return specificOpening + SECONDS_IN_DAY;
        }
        return requestedDepartureTime;
    }

    @Override
    public int latestArrivalTime(int requestedArrivalTime) {
        if (opening == null || closing == null) {
            return requestedArrivalTime;
        }

        // opening & closing is relative to the departure
        int requestedDepartureTime = requestedArrivalTime - durationInSeconds();
        int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_DAY);
        int specificOpening = days * SECONDS_IN_DAY + opening;
        int specificClosing = days * SECONDS_IN_DAY + closing;
        int closeAtArrival = specificClosing + durationInSeconds();

        if (requestedDepartureTime < specificOpening) {
            // return the closing for the previous day, offset with durationInSeconds()
            return closeAtArrival - SECONDS_IN_DAY;
        }
        else if (requestedArrivalTime > closeAtArrival) {
            return closeAtArrival;
        }
        return requestedArrivalTime;
    }

    @Override
    public String toString() {
        return asString();
    }
}
