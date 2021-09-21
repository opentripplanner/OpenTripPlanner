package org.opentripplanner.transit.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Represent a transfer leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

    private final int fromStop;
    private final int fromTime;
    private final int toStop;
    private final int toTime;
    private final int cost;
    private final RaptorTransfer transfer;
    private final PathLeg<T> next;


    public TransferPathLeg(
            int fromStop,
            int fromTime,
            int toTime,
            int generalizedCost,
            RaptorTransfer transfer,
            PathLeg<T> next
    ) {
        this.fromStop = fromStop;
        this.fromTime = fromTime;
        this.toStop = transfer.stop();
        this.toTime = toTime;
        this.cost = generalizedCost;
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
    public final int fromStop() {
        return fromStop;
    }

    @Override
    public final int fromTime() {
        return fromTime;
    }

    @Override
    public final int toStop(){
        return toStop;
    }

    @Override
    public final int toTime(){
        return toTime;
    }

    @Override
    public int generalizedCost() {
        return cost;
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
        if (o == null || getClass() != o.getClass()) { return false; }
        TransferPathLeg<?> that = (TransferPathLeg<?>) o;
        return toStop == that.toStop &&
                toTime == that.toTime &&
                fromStop == that.fromStop &&
                fromTime == that.fromTime &&
                Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toStop, toTime, fromStop, fromTime, next);
    }
}
