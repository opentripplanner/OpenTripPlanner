package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

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

    public boolean matchesFrom(T trip, int stopPos) {
        return fromTrip.equals(trip) && fromStopPos == stopPos;
    }

    public boolean matchesTo(T trip, int stopPos) {
        return toTrip == trip && toStopPos == stopPos;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(GuaranteedTransfer.class)
                .addObj("from", toString(fromTrip, fromStopPos, true))
                .addObj("to", toString(toTrip, toStopPos, false))
                .toString();
    }

    private static <T extends RaptorTripSchedule> String toString(T trip, int stopPos, boolean arrival) {
        return ValueObjectToStringBuilder.of()
                .addObj(trip.pattern().debugInfo())
                .addServiceTime(arrival ? trip.arrival(stopPos) : trip.departure(stopPos))
                .addText("[").addNum(stopPos).addText("]")
                .toString();
    }
}
