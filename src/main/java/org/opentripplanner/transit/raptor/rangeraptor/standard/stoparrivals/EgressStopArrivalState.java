package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.function.Consumer;

/**
 * The egress stop arrival state is responsible for sending arrival notifications.
 * This is used to update the destination arrivals.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class EgressStopArrivalState<T extends RaptorTripSchedule> extends StopArrivalState<T> {
    private final int round;
    private final RaptorTransfer egressLeg;
    private final Consumer<EgressStopArrivalState<T>> transitCallback;

    EgressStopArrivalState(int round, RaptorTransfer egressLeg, Consumer<EgressStopArrivalState<T>> transitCallback) {
        this.round = round;
        this.egressLeg = egressLeg;
        this.transitCallback = transitCallback;
    }

    public int round() {
        return round;
    }

    public int stop() {
        return egressLeg.stop();
    }


    public final RaptorTransfer egressLeg() {
        return egressLeg;
    }

    @Override
    public void arriveByTransit(int time, int boardStop, int boardTime, T trip) {
        super.arriveByTransit(time, boardStop, boardTime, trip);
        transitCallback.accept(this);
    }
}
