package org.opentripplanner.routing.algorithm.transferoptimization.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.TimeUtils;


/**
 * This builder is used to create a {@link Transfer} for use in unit-tests. It build a valid
 * instance with dummy trip reference.
 */
@SuppressWarnings("UnusedReturnValue")
public class TestTransferBuilder<T extends RaptorTripSchedule> {
    private final T fromTrip;
    private final int fromStopIndex;
    private final T toTrip;
    private final int toStopIndex;
    private boolean staySeated = false;
    private boolean guaranteed = false;
    private TransferPriority priority = TransferPriority.ALLOWED;
    private int maxWaitTime = Transfer.MAX_WAIT_TIME_NOT_SET;

    private TestTransferBuilder(
            T fromTrip,
            int fromStopIndex,
            T toTrip,
            int toStopIndex
    ) {
        this.fromTrip = fromTrip;
        this.fromStopIndex = fromStopIndex;
        this.toTrip = toTrip;
        this.toStopIndex = toStopIndex;
    }

    public static <T extends RaptorTripSchedule> TestTransferBuilder<T> txConstrained(
            T fromTrip,
            int fromStopIndex,
            T toTrip,
            int toStopIndex
    ) {
        return new TestTransferBuilder<>(fromTrip, fromStopIndex, toTrip, toStopIndex);
    }

    public T getFromTrip() {
        return fromTrip;
    }

    public int getFromStopIndex() {
        return fromStopIndex;
    }

    public T getToTrip() {
        return toTrip;
    }

    public int getToStopIndex() {
        return toStopIndex;
    }

    public TestTransferBuilder<T> staySeated() {
        this.staySeated = true;
        return this;
    }

    public TestTransferBuilder<T> guaranteed() {
        this.guaranteed = true;
        return this;
    }

    public TestTransferBuilder<T> maxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
        return this;
    }

    public TestTransferBuilder<T> priority(TransferPriority priority) {
        this.priority = priority;
        return this;
    }

    public Transfer build() {
        if(fromTrip == null) { throw new NullPointerException(); }
        if(toTrip == null) { throw new NullPointerException(); }

        int fromStopPos =fromTrip.pattern().findStopPositionAfter(0, fromStopIndex);
        int toStopPos = toTrip.pattern().findStopPositionAfter(0, toStopIndex);

        return new Transfer(
                new TripTransferPoint(createDummyTrip(fromTrip), fromStopPos),
                new TripTransferPoint(createDummyTrip(toTrip), toStopPos),
                priority,
                staySeated,
                guaranteed,
                maxWaitTime
        );
    }

    private static <T extends RaptorTripSchedule> Trip createDummyTrip(T trip) {
        // Set a uniq id: pattern + the first stop departure time
        return new Trip(
                new FeedScopedId(
                        trip.pattern().debugInfo(),
                        TimeUtils.timeToStrCompact(trip.departure(0))
                )
        );
    }
}
