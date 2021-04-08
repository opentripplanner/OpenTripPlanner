package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Represent an access leg in a path. The access leg is the first leg from origin to the
 * first transit leg. The next leg must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {
    private final RaptorTransfer access;
    private final int fromTime;
    private final int toStop;
    private final int toTime;
    private final int cost;
    private final PathLeg<T> next;


    public AccessPathLeg(
        @Nonnull RaptorTransfer access,
        int toStop,
        int fromTime,
        int toTime,
        int cost,
        @Nonnull PathLeg<T> next
    ) {
        this.access = access;
        this.fromTime = fromTime;
        this.toStop = toStop;
        this.toTime = toTime;
        this.cost = cost;
        this.next = next;
    }

    /** Create new access leg with a different tail */
    public AccessPathLeg(@Nonnull AccessPathLeg<T> o, @Nonnull PathLeg<T> next) {
        this(o.access, o.toStop, o.fromTime, o.toTime, o.cost, next);
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
        return toStop;
    }

    @Override
    public int toTime() {
        return toTime;
    }

    @Override
    public int generalizedCost() {
        return cost;
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
        return "Access " + asString(toStop);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        AccessPathLeg<?> that = (AccessPathLeg<?>) o;
        return fromTime == that.fromTime &&
                toStop == that.toStop &&
                toTime == that.toTime &&
                next.equals(that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTime, toStop, toTime, next);
    }
}
