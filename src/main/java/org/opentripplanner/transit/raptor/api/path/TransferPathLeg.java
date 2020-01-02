package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

import java.util.Objects;

/**
 * Represent a transfer leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public final class TransferPathLeg<T extends TripScheduleInfo> extends IntermediatePathLeg<T> {

    private final TransitPathLeg<T> next;


    public TransferPathLeg(int fromStop, int fromTime, int toStop, int toTime, TransitPathLeg<T> next) {
        super(fromStop, fromTime, toStop, toTime);
        this.next = next;
    }

    @Override
    public final boolean isTransferLeg() {
        return true;
    }

    @Override
    public final TransitPathLeg<T> nextLeg() {
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        org.opentripplanner.transit.raptor.api.path.TransferPathLeg<?> that = (org.opentripplanner.transit.raptor.api.path.TransferPathLeg<?>) o;
        return next.equals(that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), next);
    }
}
