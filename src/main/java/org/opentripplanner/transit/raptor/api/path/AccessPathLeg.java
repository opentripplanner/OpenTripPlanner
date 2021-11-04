package org.opentripplanner.transit.raptor.api.path;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Represent an access leg in a path. The access leg is the first leg from origin to the
 * first transit leg. The next leg must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {
    private final RaptorTransfer access;
    private final int fromTime;
    private final int toTime;
    private final int generalizedCost;
    private final PathLeg<T> next;


    public AccessPathLeg(
        @Nonnull RaptorTransfer access,
        int fromTime,
        int toTime,
        int generalizedCost,
        @Nonnull PathLeg<T> next
    ) {
        this.access = access;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.generalizedCost = generalizedCost;
        this.next = next;
    }

    @Override
    public int fromTime() {
        return fromTime;
    }

    /**
     * The stop index where the leg end, also called arrival stop index.
     */
    @Override
    public int toStop() {
        return access.stop();
    }

    @Override
    public int toTime() {
        return toTime;
    }

    @Override
    public int generalizedCost() {
        return generalizedCost;
    }

    @Override
    public boolean isAccessLeg() {
        return true;
    }

    public RaptorTransfer access() {
        return access;
    }

    @Override
    public PathLeg<T> nextLeg() {
        return next;
    }

    @Override
    public String toString() {
        return "Access " + asString(toStop());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        AccessPathLeg<?> that = (AccessPathLeg<?>) o;
        return fromTime == that.fromTime &&
                toStop() == that.toStop() &&
                toTime == that.toTime &&
                next.equals(that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTime, toStop(), toTime, next);
    }
}
