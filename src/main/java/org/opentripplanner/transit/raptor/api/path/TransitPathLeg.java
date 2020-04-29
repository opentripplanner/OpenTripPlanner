package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Objects;

/**
 * Represent a transit leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitPathLeg<T extends RaptorTripSchedule> extends IntermediatePathLeg<T> {

    private final PathLeg<T> next;
    private final T trip;

    public TransitPathLeg(int fromStop, int fromTime, int toStop, int toTime, T trip, PathLeg<T> next) {
        super(fromStop, fromTime, toStop, toTime);
        this.next = next;
        this.trip = trip;
    }

    /**
     * The trip schedule info object passed into Raptor routing algorithm. 
     */
    public T trip() {
        return trip;
    }

    @Override
    public final boolean isTransitLeg() {
        return true;
    }

    @Override
    public final PathLeg<T> nextLeg() {
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        TransitPathLeg<?> that = (TransitPathLeg<?>) o;
        return trip.equals(that.trip) && next.equals(that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), next, trip);
    }

    @Override
    public String toString() {
        return trip.debugInfo() + " " + asString(toStop());
    }
}
