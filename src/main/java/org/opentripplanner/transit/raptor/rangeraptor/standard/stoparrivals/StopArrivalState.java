package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public interface StopArrivalState<T extends RaptorTripSchedule> {

    /** The overall best time to reach this stop */
    int time();

    /**
     * The best time to reach this stop on board a vehicle, it may be by
     * transit or by flex access.
     */
    int onBoardArrivalTime();

    /** Stop arrival reached by transit or on-board access. */
    boolean reachedOnBoard();

    /** Stop arrival reached, at least one time (any round/iteration). */
    boolean reachedOnStreet();


    /* Access */

    /**
     * Return true is the best option is an access arrival.
     */
    boolean arrivedByAccessOnStreet();

    /**
     * Return the access path for the best stop arrival.
     */
    RaptorTransfer accessPathOnStreet();

    /**
     * Return true is the best option is an access arrival.
     */
    boolean arrivedByAccessOnBoard();

    /**
     * Return the access path for the best stop arrival.
     */
    RaptorTransfer accessPathOnBoard();


    /* Transit */

    /**
     * A transit arrival exist, but it might be a better transfer arrival as well.
     */
    boolean arrivedByTransit();

    T trip();

    int boardTime();

    int boardStop();

    void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip);

    void setBestTimeTransit(int time);


    /* Transfer */

    /**
     * The best arrival is by transfer.
     */
    boolean arrivedByTransfer();

    int transferFromStop();

    RaptorTransfer transferPath();

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the
     * transfer time
     */
    void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath);


    static <T extends RaptorTripSchedule> StopArrivalState<T> create() {
        return new DefaultStopArrivalState<>();
    }
}
