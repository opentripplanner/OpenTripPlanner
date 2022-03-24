package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import org.opentripplanner.transit.raptor.rangeraptor.standard.DestinationArrivalListener;

/**
 * The egress stop arrival state is responsible for sending arrival notifications.
 * This is used to update the destination arrivals.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class EgressStopArrivalState<T extends RaptorTripSchedule> extends DefaultStopArrivalState<T> {
    private final int round;
    private final int stop;
    private final RaptorTransfer[] egressPaths;
    private final DestinationArrivalListener callback;

    EgressStopArrivalState (
            int stop,
            int round,
            Collection<RaptorTransfer> egressPaths,
            DestinationArrivalListener transitCallback
    ) {
        this.round = round;
        this.stop = stop;
        this.egressPaths = egressPaths.toArray(new RaptorTransfer[0]);
        this.callback = transitCallback;
    }

    public int round() {
        return round;
    }

    public int stop() {
        return stop;
    }

    @Override
    public void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath) {
        super.transferToStop(fromStop, arrivalTime, transferPath);
        for (RaptorTransfer egressPath : egressPaths) {
            if(egressPath.stopReachedOnBoard()) {
                callback.newDestinationArrival(round, arrivalTime, transferPath.stopReachedOnBoard(), egressPath);
            }
        }
    }

    @Override
    public void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip) {
        super.arriveByTransit(arrivalTime, boardStop, boardTime, trip);
        for (RaptorTransfer egressPath : egressPaths) {
            callback.newDestinationArrival(round, arrivalTime, true, egressPath);
        }
    }

    @Override
    public String toString() {
        var builder = ToStringBuilder.of(EgressStopArrivalState.class)
                .addNum("stop", stop)
                .addNum("round", round);
        // Add super type fields
        toStringAddBody(builder);
        // Add egress stop last (collection)
        builder.addCol("egressPaths", List.of(egressPaths));
        return builder.toString();
    }
}
