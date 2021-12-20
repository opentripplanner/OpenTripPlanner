package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.Collection;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

class ConstrainedBoardingSearchReverse
        implements ConstrainedBoardingSearchStrategy {

    @Override
    public TransferPoint source(ConstrainedTransfer tx) {return tx.getTo();}

    @Override
    public TransferPoint target(ConstrainedTransfer tx) {return tx.getFrom();}

    @Override
    public int time(RaptorTripSchedule schedule, int stopPos) {
        return schedule.arrival(stopPos);
    }

    @Override
    public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
        return schedule.findDepartureStopPosition(timeLimit, stop);
    }

    @Override
    public T2<Integer, TransferConstraint> findTimetableTripInfo(
            RaptorTimeTable<TripSchedule> timetable,
            Collection<ConstrainedTransfer> transfers,
            int stopPos,
            int sourceDepartureTime
    ) {
        // Abort after 6 hours
        boolean alightPrevNormalTrip = false;

        outer:
        for (int i = timetable.numberOfTripSchedules()-1; i >= 0; --i) {
            var it = timetable.getTripSchedule(i);
            int arrivalTime = it.arrival(stopPos);
            if (arrivalTime > sourceDepartureTime) { continue; }

            var targetTrip = it.getOriginalTripTimes().getTrip();

            for (ConstrainedTransfer tx : transfers) {
                if (tx.getFrom().applyToAllTrips()) {
                    return new T2<>(i, tx.getTransferConstraint());
                }
                if (targetTrip == tx.getFrom().getTrip()) {
                    if (tx.getTransferConstraint().isNotAllowed()) {
                        alightPrevNormalTrip = true;
                        continue outer;
                    }
                    else {
                        return new T2<>(i, tx.getTransferConstraint());
                    }
                }
            }
            if (alightPrevNormalTrip) {
                return new T2<>(i, TransferConstraint.REGULAR_TRANSFER);
            }
        }
        return null;
    }
}
