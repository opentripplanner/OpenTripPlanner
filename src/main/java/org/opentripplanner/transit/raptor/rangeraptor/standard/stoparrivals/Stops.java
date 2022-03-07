package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.DestinationArrivalListener;
import org.opentripplanner.transit.raptor.rangeraptor.transit.EgressPaths;

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
        this.stops = (DefaultStopArrivalState<T>[][]) new DefaultStopArrivalState[nRounds][nStops];
    }

    /**
     * Setup egress arrivals with a callback which is notified when a new transit egress arrival happens.
     */
    public void setupEgressStopStates(
            EgressPaths egressPaths,
            DestinationArrivalListener destinationArrivalListener
    ) {
        for (int i = 1; i < stops.length; i++) {
            final int round = i;
            egressPaths.byStop().forEachEntry((stop, list) -> {
                stops[round][stop] = new EgressStopArrivalState<>(
                        stop, round, list, destinationArrivalListener
                );
                return true;
            });
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
                return i - 1;
            }
        }
        return unreachedMinNumberOfTransfers();
    }

    void setAccessTime(int time, RaptorTransfer access) {
        final int stop = access.stop();
        var other = stops[round()][stop];
        if (other == null) {
            stops[round()][stop] = new AccessStopArrivalState<>(time, access);
        } else {
            stops[round()][stop] = new AccessStopArrivalState<T>(time, access, other);
        }
    }

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the transfer time
     */
    void transferToStop(int fromStop, RaptorTransfer transfer, int arrivalTime) {
        int stop = transfer.stop();
        var state = findOrCreateStopIndex(round(), stop);

        state.transferToStop(fromStop, arrivalTime, transfer);
    }

    void transitToStop(int stop, int time, int boardStop, int boardTime, T trip, boolean bestTime) {
        var state = findOrCreateStopIndex(round(), stop);

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
            stops[round][stop] = new DefaultStopArrivalState<>();
        }
        return get(round, stop);
    }

    private int round() {
        return roundProvider.round();
    }

    TransitArrival<T> previousTransit(int boardStopIndex) {
        final int prevRound = round() - 1;
        int stopIndex = boardStopIndex;
        StopArrivalState<T> state = get(prevRound, boardStopIndex);

        // We check for transfer before access, since a FLEX arrive on-board
        // can be followed by a transfer
        if(state.arrivedByTransfer()) {
            stopIndex = state.transferFromStop();
            state = stops[prevRound][stopIndex];
        }
        if(state.arrivedByAccess()) { return null; }
        
        return TransitArrival.create(state.trip(), stopIndex, state.transitArrivalTime());
    }
}
