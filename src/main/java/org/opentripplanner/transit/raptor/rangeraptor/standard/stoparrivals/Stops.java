package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;

import java.util.function.Consumer;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class Stops<T extends RaptorTripSchedule> implements BestNumberOfTransfers {

    private final StopArrivalState<T>[][] stops;
    private final RoundProvider roundProvider;

    public Stops(
            int nRounds,
            int nStops,
            RoundProvider roundProvider
    ) {
        this.roundProvider = roundProvider;
        //noinspection unchecked
        this.stops = (StopArrivalState<T>[][]) new StopArrivalState[nRounds][nStops];
    }

    /**
     * Setup egress arrivals with a callback witch is notified when a new transit egress arrival happens.
     */
    public void setupEgressStopStates(
            Iterable<RaptorTransfer> egressPaths,
            Consumer<EgressStopArrivalState<T>> transitArrivalCallback
    ) {
        for (int round = 1; round < stops.length; round++) {
            for (RaptorTransfer egressPath : egressPaths) {
                if(stops[round][egressPath.stop()] == null) {
                    EgressStopArrivalState<T> state = new EgressStopArrivalState<>(
                        round,
                        egressPath,
                        transitArrivalCallback
                    );
                    stops[round][egressPath.stop()] = state;
                }
                else {
                    throw new IllegalStateException(""
                        + "Currently Raptor do not support multiple access/egress paths to the "
                        + "same stop. If this exception occurs and OTP was serving a normal "
                        + "use-case, then this needs to be fixed. For example this needs to be "
                        + "fixed if OTP should support more than on access/egress mode. "
                        + "See issue #3300. Details: "
                        + "State exist for stop: " + egressPath.stop() + ", round: " + round
                    );
                }
            }
        }
    }

    public boolean exist(int round, int stop) {
        StopArrivalState<T> s = get(round, stop);
        return s != null && s.reached();
    }

    public StopArrivalState<T> get(int round, int stop) {
        return stops[round][stop];
    }

    @Override
    public int calculateMinNumberOfTransfers(int stop) {
        for (int i = 0; i < stops.length; i++) {
            if(stops[i][stop] != null) {
                return i;
            }
        }
        return unreachedMinNumberOfTransfers();
    }

    void setAccessTime(int time, RaptorTransfer access) {
        final int stop = access.stop();
        if (stops[round()][stop] == null) {
            stops[round()][stop] = new AccessStopArrivalState<>(time, access);
        } else {
            stops[round()][stop] = new AccessStopArrivalState<>(time, access, stops[round()][stop]);
        }
    }

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the transfer time
     */
    void transferToStop(int fromStop, RaptorTransfer transfer, int arrivalTime) {
        int stop = transfer.stop();
        StopArrivalState<T> state = findOrCreateStopIndex(round(), stop);

        state.transferToStop(fromStop, arrivalTime, transfer);
    }

    void transitToStop(int stop, int time, int boardStop, int boardTime, T trip, boolean bestTime) {
        StopArrivalState<T> state = findOrCreateStopIndex(round(), stop);

        state.arriveByTransit(time, boardStop, boardTime, trip);

        if (bestTime) {
            state.setBestTimeTransit(time);
        }
    }

    int bestTimePreviousRound(int stop) {
        return get(round() - 1, stop).time();
    }


    /* private methods */

    private StopArrivalState<T> findOrCreateStopIndex(final int round, final int stop) {
        if (stops[round][stop] == null) {
            stops[round][stop] = new StopArrivalState<>();
        }
        return get(round, stop);
    }

    private int round() {
        return roundProvider.round();
    }
}
