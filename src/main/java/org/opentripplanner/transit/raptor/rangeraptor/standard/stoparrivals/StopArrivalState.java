package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.IntUtils;
import org.opentripplanner.transit.raptor.util.TimeUtils;


/**
 * This class main purpose is to hold data for a given arrival at a stop and raptor round. It should be as light
 * weight as possible to minimize memory consumption and cheap to create and garbage collect.
 * <p/>
 * This class holds both the best transit and the best transfer to a stop if they exist for a given round and stop.
 * The normal case is that this class represent either a transit arrival or a transfer arrival. We only keep both
 * if the transfer is better, arriving before the transit.
 * <p/>
 * The reason we need to keep both the best transfer and the best transit for a given stop and round is that
 * we may arrive at a stop by transit, then in the same or later round we may arrive by transit. If the transfer
 * arrival is better then the transit arrival it might be tempting to remove the transit arrival, but this
 * transit might be the best way (or only way) to get to another stop by transfer.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StopArrivalState<T extends RaptorTripSchedule> {

    /**
     * Used to initialize all none time based attributes.
     */
    private static final int NOT_SET = -1;


    // Best time - access, transit or transfer
    private int bestArrivalTime = NOT_SET;

    // Transit
    private int transitArrivalTime = NOT_SET;
    private T trip = null;
    private int boardTime = NOT_SET;
    private int boardStop = NOT_SET;

    // Transfer (and access)
    private int transferFromStop = NOT_SET;
    private int accessOrTransferDuration = NOT_SET;

    public final int time() {
        return bestArrivalTime;
    }

    public final int accessDuration() {
        return accessOrTransferDuration;
    }

    public final int transitTime() {
        return transitArrivalTime;
    }

    public final T trip() {
        return trip;
    }

    public final int boardTime() {
        return boardTime;
    }

    public final int boardStop() {
        return boardStop;
    }

    public final int transferFromStop() {
        return transferFromStop;
    }

    public final int transferDuration() {
        return accessOrTransferDuration;
    }

    public final boolean arrivedByTransit() {
        return transitArrivalTime != NOT_SET;
    }

    public final boolean arrivedByTransfer() {
        return transferFromStop != NOT_SET;
    }

    void setAccessTime(int time, int accessDuration) {
        this.bestArrivalTime = time;
        this.accessOrTransferDuration = accessDuration;
    }

    final boolean reached() {
        return bestArrivalTime != NOT_SET;
    }

    public void arriveByTransit(int time, int boardStop, int boardTime, T trip) {
        this.transitArrivalTime = time;
        this.trip = trip;
        this.boardTime = boardTime;
        this.boardStop = boardStop;
    }

    final void setBestTimeTransit(int time) {
        this.bestArrivalTime = time;
        // The transfer is cleared since it is not the fastest alternative any more.
        this.transferFromStop = NOT_SET;
    }

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the transfer time
     */
    public final void transferToStop(int fromStop, int arrivalTime, int transferTime) {
        this.bestArrivalTime = arrivalTime;
        this.transferFromStop = fromStop;
        this.accessOrTransferDuration = transferTime;
    }

    public AccessStopArrivalState<T> asAccessStopArrivalState() {
        return (AccessStopArrivalState<T>) this;
    }

    @Override
    public String toString() {
        return String.format("Arrival { time: %s, Transit: %s %s-%s, trip: %s, Transfer from: %s %s }",
                TimeUtils.timeToStrCompact(bestArrivalTime, NOT_SET),
                IntUtils.intToString(boardStop, NOT_SET),
                TimeUtils.timeToStrCompact(boardTime, NOT_SET),
                TimeUtils.timeToStrCompact(transitArrivalTime, NOT_SET),
                trip == null ? "" : trip.debugInfo(),
                IntUtils.intToString(transferFromStop, NOT_SET),
                TimeUtils.durationToStr(accessOrTransferDuration, NOT_SET)
        );
    }
}
