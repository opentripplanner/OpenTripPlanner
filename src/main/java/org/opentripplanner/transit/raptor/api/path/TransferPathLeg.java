package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Objects;

/**
 * Represent a transfer leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferPathLeg<T extends RaptorTripSchedule> extends IntermediatePathLeg<T> {

    private final PathLeg<T> next;

    private final RaptorTransfer transfer;

    public TransferPathLeg(int fromStop, int fromTime, int toStop, int toTime, int cost, RaptorTransfer transfer, PathLeg<T> next) {
        super(fromStop, fromTime, toStop, toTime, cost);
        this.transfer = transfer;
        this.next = next;
    }

    public final RaptorTransfer transfer() {
        return transfer;
    }

    @Override
    public final boolean isTransferLeg() {
        return true;
    }

    @Override
    public final PathLeg<T> nextLeg() {
        return next;
    }

    @Override
    public String toString() {
        return "Walk " + asString(toStop());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!super.equals(o)) { return false; }
        TransferPathLeg<?> that = (TransferPathLeg<?>) o;
        return next.equals(that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), next);
    }
}
