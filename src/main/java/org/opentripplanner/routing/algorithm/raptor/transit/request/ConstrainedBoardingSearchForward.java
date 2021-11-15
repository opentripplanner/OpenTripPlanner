package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.Collection;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

class ConstrainedBoardingSearchForward
        implements ConstrainedBoardingSearchStrategy {

    @Override
    public TransferPoint source(ConstrainedTransfer tx) {return tx.getFrom();}

    @Override
    public TransferPoint target(ConstrainedTransfer tx) {return tx.getTo();}

    @Override
    public int time(RaptorTripSchedule schedule, int stopPos) {
        return schedule.departure(stopPos);
    }

    @Override
    public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
        return schedule.findArrivalStopPosition(timeLimit, stop);
    }

    @Override
    public T2<Integer, TransferConstraint> findTimetableTripInfo(
            RaptorTimeTable<TripSchedule> timetable,
            Collection<ConstrainedTransfer> transfers,
            int stopPos,
            int sourceArrivalTime
    ) {
        // Abort after 6 hours
        boolean boardNextNormalTrip = false;

        outer:
        for (int i = 0; i < timetable.numberOfTripSchedules(); ++i) {
            var it = timetable.getTripSchedule(i);
            int departureTime = it.departure(stopPos);
            if (departureTime < sourceArrivalTime) { continue; }

            var targetTrip = it.getOriginalTripTimes().getTrip();

            for (ConstrainedTransfer tx : transfers) {
                if (tx.getTo().applyToAllTrips()) {
                    return new T2<>(i, tx.getTransferConstraint());
                }
                if (targetTrip == tx.getTo().getTrip()) {
                    if (tx.getTransferConstraint().isNotAllowed()) {
                        boardNextNormalTrip = true;
                        continue outer;
                    }
                    else {
                        return new T2<>(i, tx.getTransferConstraint());
                    }
                }
            }
            if (boardNextNormalTrip) {
                return new T2<>(i, null);
            }
        }
        return null;
    }
}
