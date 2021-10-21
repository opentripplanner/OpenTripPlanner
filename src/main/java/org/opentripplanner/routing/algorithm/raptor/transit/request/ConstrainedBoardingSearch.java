package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.List;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;


/**
 * The responsibility of this class is to provide transfer constraints to the Raptor search
 * for a given pattern. The instance is stateful and not thread-safe. The current stop
 * position is checked for transfers, then the provider is asked to list all transfers
 * between the current pattern and the source trip stop arrival. The source is the "from"
 * point in a transfer for a forward search, and the "to" point in the reverse search.
 */
public final class ConstrainedBoardingSearch
        implements RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> {

    private static final int NOT_FOUND = -999_999_999;
    private static final DirectionHelper FORWARD_HELPER = new ForwardDirectionHelper();
    private static final DirectionHelper REVERSE_HELPER = new ReverseDirectionHelper();

    private final DirectionHelper translator;

    /**
     * List of transfers for each stop position in pattern
     */
    private final TIntObjectMap<List<ConstrainedTransfer>> transfers;

    private List<ConstrainedTransfer> currentTransfers;

    public ConstrainedBoardingSearch(
            boolean forwardSearch,
            TIntObjectMap<List<ConstrainedTransfer>> transfers
    ) {
        this.translator = forwardSearch ? FORWARD_HELPER : REVERSE_HELPER;
        this.transfers = transfers;
    }

    @Override
    public final boolean transferExist(int targetStopPos) {
        if(transfers == null) { return false; }

        // Get all guaranteed transfers for the target pattern at the target stop position
        this.currentTransfers = transfers.get(targetStopPos);
        return currentTransfers != null;
    }

    @Override
    public final RaptorTripScheduleBoardOrAlightEvent<TripSchedule> find(
            RaptorTimeTable<TripSchedule> timetable,
            TripSchedule sourceTripSchedule,
            int sourceStopIndex,
            int sourceArrivalTime
    ) {
        final Trip sourceTrip = sourceTripSchedule.getOriginalTripTimes().getTrip();
        final int sourceStopPos = translator.findSourceStopPosition(
                sourceTripSchedule, sourceArrivalTime, sourceStopIndex
        );

        ConstrainedTransfer tx = findMatchingTargetPoint(sourceTrip, sourceStopPos);

        if(tx == null) { return null; }

        TransferPoint target = translator.target(tx);
        final int targetStopPos = target.getStopPosition();

        int tripIndex = translator.findTimetableTripIndex(
                timetable,
                target.getTrip(),
                targetStopPos,
                sourceArrivalTime
        );
        if(tripIndex == NOT_FOUND) { return null; }

        var trip = timetable.getTripSchedule(tripIndex);
        int departureTime = translator.time(trip, targetStopPos);

        return new ConstrainedTransferBoarding<>(
                tx.getTransferConstraint(), tripIndex, trip, targetStopPos, departureTime
        );
    }

    private ConstrainedTransfer findMatchingTargetPoint(
            Trip sourceTrip,
            int sourceStopPos
    ) {
        for (ConstrainedTransfer tx : currentTransfers) {
            var sourcePoint = translator.source(tx);
            if(sourcePoint.matches(sourceTrip, sourceStopPos)) {
                return tx;
            }
        }
        return null;
    }

    private interface DirectionHelper {
        TransferPoint source(ConstrainedTransfer tx);
        TransferPoint target(ConstrainedTransfer tx);
        int time(RaptorTripSchedule schedule, int stopPos);
        int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop);
        int findTimetableTripIndex(
                RaptorTimeTable<TripSchedule> timetable,
                Trip trip,
                int stopPos,
                int sourceTime
        );
    }

    private static class ForwardDirectionHelper implements DirectionHelper {
        @Override public TransferPoint source(ConstrainedTransfer tx) { return tx.getFrom();  }
        @Override public TransferPoint target(ConstrainedTransfer tx) { return tx.getTo(); }
        @Override public int time(RaptorTripSchedule schedule, int stopPos) {
            return schedule.departure(stopPos);
        }
        @Override
        public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
            return schedule.findArrivalStopPosition(timeLimit, stop);
        }
        @Override
        public int findTimetableTripIndex(
                RaptorTimeTable<TripSchedule> timetable,
                Trip trip,
                int stopPos,
                int sourceArrivalTime
        ) {
            // Abort after 6 hours
            int maxLimit = sourceArrivalTime + 3600 * 6;

            for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
                var it = timetable.getTripSchedule(i);
                int departureTime = it.departure(stopPos);
                if(departureTime < sourceArrivalTime) { continue; }
                if(departureTime > maxLimit) { return NOT_FOUND; }
                if(it.getOriginalTripTimes().getTrip() == trip) { return i; }
            }
            return NOT_FOUND;
        }
    }

    private static class ReverseDirectionHelper implements DirectionHelper {
        @Override public TransferPoint source(ConstrainedTransfer tx) { return tx.getTo();  }
        @Override public TransferPoint target(ConstrainedTransfer tx) { return tx.getFrom(); }
        @Override public int time(RaptorTripSchedule schedule, int stopPos) {
            return schedule.arrival(stopPos);
        }
        @Override
        public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
            return schedule.findDepartureStopPosition(timeLimit, stop);
        }
        @Override
        public int findTimetableTripIndex(
                RaptorTimeTable<TripSchedule> timetable,
                Trip trip,
                int stopPos,
                int sourceDepartureTime
        ) {
            // Abort after 6 hours
            int minLimit = sourceDepartureTime - 3600 * 6;

            for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
                var it = timetable.getTripSchedule(i);
                int arrivalTime = it.arrival(stopPos);
                if(arrivalTime < minLimit) { continue; }
                if(arrivalTime > sourceDepartureTime) { return NOT_FOUND; }
                if(it.getOriginalTripTimes().getTrip() == trip) { return i; }
            }
            return NOT_FOUND;
        }
    }
}
