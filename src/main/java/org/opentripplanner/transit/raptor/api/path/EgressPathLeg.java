package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Objects;

/**
 * Represent a egress leg in a path. The egress leg is the last leg arriving at the destination. The previous leg
 * must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class EgressPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {
    private final RaptorTransfer egress;
    private final int fromStop;
    private final int fromTime;
    private final int toTime;

    public EgressPathLeg(RaptorTransfer egress, int fromStop, int fromTime, int toTime) {
        this.egress = egress;
        this.fromStop = fromStop;
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    /**
     * The stop index where the leg start, also called the leg departure stop index.
     */
    public final int fromStop() {
        return fromStop;
    }

    @Override
    public final int fromTime() {
        return fromTime;
    }

    @Override
    public final int toTime() {
        return toTime;
    }

    public RaptorTransfer egress() {
        return egress;
    }

    /**
     * @throws UnsupportedOperationException - an egress leg is the last leg in a path and does not have a next leg.
     */
    @Override
    public final TransitPathLeg<T> nextLeg() {
        throw new java.lang.UnsupportedOperationException(
                "The egress leg is the last leg in a path. Use isEgressLeg() to identify las leg."
        );
    }

    @Override
    public final boolean isEgressLeg() {
        return true;
    }

    @Override
    public String toString() {
        return "Egress " + asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EgressPathLeg<?> that = (EgressPathLeg<?>) o;
        return fromStop == that.fromStop &&
                fromTime == that.fromTime &&
                toTime == that.toTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStop, fromTime, toTime);
    }
}
