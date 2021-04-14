package org.opentripplanner.transit.raptor.api.transit;

import org.opentripplanner.model.base.ToStringBuilder;

/**
 *
 * <h3>Implementation notes</h3>
 * This is implemented as a class. It might make sense to use an interface instead,
 * but it is overkill for now.
 */
public class GuaranteedTransfer<T extends RaptorTripSchedule> {
    private final T fromTrip;
    private final int fromStopPos;
    private final T toTrip;
    private final int toStopPos;

    public GuaranteedTransfer(T fromTrip, int fromStopPos, T toTrip, int toStopPos) {
        this.fromTrip = fromTrip;
        this.fromStopPos = fromStopPos;
        this.toTrip = toTrip;
        this.toStopPos = toStopPos;
    }

    public T getFromTrip() {
        return fromTrip;
    }

    public int getFromStopPos() {
        return fromStopPos;
    }

    public T getToTrip() {
        return toTrip;
    }

    public int getToStopPos() {
        return toStopPos;
    }

    public boolean fromTripMatches(TransitArrival<T> from) {
        T fromTrip = getFromTrip();
        if(fromTrip != from.trip()) { return false; }
        return fromTrip.findArrivalStopPosition(from.arrivalTime(), from.stop()) == getFromStopPos();
    }

    public boolean toTripMatches(TransitArrival<T> to) {
        T toTrip = getToTrip();
        if(toTrip != to.trip()) { return false; }
        return toTrip.findDepartureStopPosition(to.arrivalTime(), to.stop()) == getToStopPos();
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(GuaranteedTransfer.class)
                .addObj("toTrip", toTrip)
                .addNum("toStopPos", toStopPos)
                .addObj("fromTrip", fromTrip)
                .addNum("fromStopPos", fromStopPos)
                .toString();
    }
}
