package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public interface StopArrivalState<T extends RaptorTripSchedule> {

    int time();

    boolean reached();

    boolean arrivedByAccess();

    RaptorTransfer accessPath();

    /**
     * A transit arrival exist, but it might be a better transfer arrival as well.
     */
    boolean arrivedByTransit();

    int transitArrivalTime();

    T trip();

    int boardTime();

    int boardStop();

    int transferFromStop();

    int transferDuration();


    /**
     * The best arrival is by transfer.
     */
    boolean arrivedByTransfer();

    RaptorTransfer transferPath();


    void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip);

    void setBestTimeTransit(int time);

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the
     * transfer time
     */
    void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath);


    static <T extends RaptorTripSchedule> StopArrivalState<T> create() {
        return new DefaultStopArrivalState<>();
    }
}
