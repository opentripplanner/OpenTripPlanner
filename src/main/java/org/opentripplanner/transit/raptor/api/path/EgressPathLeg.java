package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

import java.util.Objects;

/**
 * Represent a egress leg in a path. The egress leg is the last leg arriving at the destination. The previous leg
 * must be a transit leg - no other legs are allowed.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public final class EgressPathLeg<T extends TripScheduleInfo> implements PathLeg<T> {
    private final int fromStop;
    private final int fromTime;
    private final int toTime;

    public EgressPathLeg(int fromStop, int fromTime, int toTime) {
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
