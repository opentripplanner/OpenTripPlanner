package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.Collection;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

interface ConstrainedBoardingSearchStrategy {

    TransferPoint source(ConstrainedTransfer tx);

    TransferPoint target(ConstrainedTransfer tx);

    int time(RaptorTripSchedule schedule, int stopPos);

    int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop);

    /**
     * Find the trip to board (trip index) and the transfer constraint
     */
    T2<Integer, TransferConstraint> findTimetableTripInfo(
            RaptorTimeTable<TripSchedule> timetable,
            Collection<ConstrainedTransfer> transfers,
            int stopPos,
            int sourceTime
    );
}
