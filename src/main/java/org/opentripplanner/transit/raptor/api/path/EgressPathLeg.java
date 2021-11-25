package org.opentripplanner.transit.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Represent a egress leg in a path. The egress leg is the last leg arriving at the destination. The previous leg
 * must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class EgressPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {
    private final RaptorTransfer egress;
    private final int fromTime;
    private final int toTime;
    private final int generalizedCost;

    public EgressPathLeg(RaptorTransfer egress, int fromTime, int toTime, int generalizedCost) {
        this.egress = egress;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.generalizedCost = generalizedCost;
    }

    /**
     * The stop index where the leg start, also called the leg departure stop index.
     */
    @Override
    public int fromStop() {
        return egress.stop();
    }

    @Override
    public int fromTime() {
        return fromTime;
    }

    @Override
    public int toTime() {
        return toTime;
    }

    @Override
    public int generalizedCost() {
        return generalizedCost;
    }

    public RaptorTransfer egress() {
        return egress;
    }

    /**
     * @throws UnsupportedOperationException - an egress leg is the last leg in a path and does not have a next leg.
     */
    @Override
    public TransitPathLeg<T> nextLeg() {
        throw new java.lang.UnsupportedOperationException(
                "The egress leg is the last leg in a path. Use isEgressLeg() to identify last leg."
        );
    }

    @Override
    public boolean isEgressLeg() {
        return true;
    }

    @Override
    public String toString() {
        return "Egress " + asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        EgressPathLeg<?> that = (EgressPathLeg<?>) o;
        return fromStop() == that.fromStop() &&
                fromTime == that.fromTime &&
                toTime == that.toTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStop(), fromTime, toTime);
    }
}
