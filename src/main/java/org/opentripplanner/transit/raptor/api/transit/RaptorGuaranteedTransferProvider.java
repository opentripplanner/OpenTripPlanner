package org.opentripplanner.transit.raptor.api.transit;


import javax.annotation.Nullable;

public interface RaptorGuaranteedTransferProvider<T extends RaptorTripSchedule> {


    /**
     * Check if the
     *
     * @param targetStopPos
     * @return
     */
    boolean transferExist(int targetStopPos);

    /**
     *
     * @param sourceTrip
     * @param sourceStopIndex
     * @param sourceArrivalTime
     * @return
     */
    @Nullable
    RaptorTripScheduleBoardOrAlightEvent<T> find(
            RaptorTimeTable<T> timetable,
            T sourceTrip,
            int sourceStopIndex,
            int sourceArrivalTime
    );
}
