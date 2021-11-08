package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
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

    private static final DirectionHelper FORWARD_HELPER = new ForwardDirectionHelper();
    private static final DirectionHelper REVERSE_HELPER = new ReverseDirectionHelper();

    private final DirectionHelper translator;

    /**
     * List of transfers for each stop position in pattern
     */
    private final TIntObjectMap<List<ConstrainedTransfer>> transfers;

    private List<ConstrainedTransfer> currentTransfers;
    private int currentTargetStopPos;

    public ConstrainedBoardingSearch(
            boolean forwardSearch,
            TIntObjectMap<List<ConstrainedTransfer>> transfers
    ) {
        this.translator = forwardSearch ? FORWARD_HELPER : REVERSE_HELPER;
        this.transfers = transfers;
    }

    @Override
    public boolean transferExist(int targetStopPos) {
        if(transfers == null) { return false; }

        // Get all guaranteed transfers for the target pattern at the target stop position
        this.currentTransfers = transfers.get(targetStopPos);
        this.currentTargetStopPos = targetStopPos;
        return currentTransfers != null;
    }

    @Override
    public RaptorTripScheduleBoardOrAlightEvent<TripSchedule> find(
            RaptorTimeTable<TripSchedule> timetable,
            TripSchedule sourceTripSchedule,
            int sourceStopIndex,
            int sourceArrivalTime
    ) {
        final Trip sourceTrip = sourceTripSchedule.getOriginalTripTimes().getTrip();
        final int sourceStopPos = translator.findSourceStopPosition(
                sourceTripSchedule, sourceArrivalTime, sourceStopIndex
        );

        var list = findMatchingTransfers(sourceTrip, sourceStopPos);

        if(list.isEmpty()) { return null; }

        var tripInfo = translator.findTimetableTripInfo(
                timetable,
                list,
                currentTargetStopPos,
                sourceArrivalTime
        );

        if(tripInfo == null) { return null; }

        final int tripIndex = tripInfo.first;
        final TransferConstraint transferConstraint = tripInfo.second;

        var trip = timetable.getTripSchedule(tripIndex);
        int departureTime = translator.time(trip, currentTargetStopPos);

        return new ConstrainedTransferBoarding<>(
                transferConstraint, tripIndex, trip, currentTargetStopPos, departureTime
        );
    }

    private Collection<ConstrainedTransfer> findMatchingTransfers(
            Trip sourceTrip,
            int sourceStopPos
    ) {
        var result = new ArrayList<ConstrainedTransfer>();
        for (ConstrainedTransfer tx : currentTransfers) {
            var sourcePoint = translator.source(tx);
            if(sourcePoint.matches(sourceTrip, sourceStopPos)) {
                result.add(tx);
            }
        }
        return result;
    }

    private interface DirectionHelper {
        TransferPoint source(ConstrainedTransfer tx);
        TransferPoint target(ConstrainedTransfer tx);
        int time(RaptorTripSchedule schedule, int stopPos);
        int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop);
        /** Find the trip to board (trip index) and the transfer constraint */
        T2<Integer, TransferConstraint> findTimetableTripInfo(
                RaptorTimeTable<TripSchedule> timetable,
                Collection<ConstrainedTransfer> transfers,
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
        public T2<Integer, TransferConstraint> findTimetableTripInfo(
                RaptorTimeTable<TripSchedule> timetable,
                Collection<ConstrainedTransfer> transfers,
                int stopPos,
                int sourceArrivalTime
        ) {
            // Abort after 6 hours
            int maxLimit = sourceArrivalTime + 3600 * 6;

            for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
                var it = timetable.getTripSchedule(i);
                int departureTime = it.departure(stopPos);
                if(departureTime < sourceArrivalTime) { continue; }
                if(departureTime > maxLimit) { return null; }

                var targetTrip = it.getOriginalTripTimes().getTrip();

                for (ConstrainedTransfer tx : transfers) {
                    if(targetTrip == tx.getTo().getTrip()) {
                        return new T2<>(i, tx.getTransferConstraint());
                    }
                }
            }
            return null;
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
        public T2<Integer, TransferConstraint> findTimetableTripInfo(
                RaptorTimeTable<TripSchedule> timetable,
                Collection<ConstrainedTransfer> transfers,
                int stopPos,
                int sourceDepartureTime
        ) {
            // Abort after 6 hours
            int minLimit = sourceDepartureTime - 3600 * 6;

            for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
                var it = timetable.getTripSchedule(i);
                int arrivalTime = it.arrival(stopPos);
                if(arrivalTime < minLimit) { continue; }
                if(arrivalTime > sourceDepartureTime) { return null; }

                var targetTrip = it.getOriginalTripTimes().getTrip();

                for (ConstrainedTransfer tx : transfers) {
                    if(targetTrip == tx.getFrom().getTrip()) {
                        return new T2<>(i ,tx.getTransferConstraint());
                    }
                }
           }
            return null;
        }
    }
}
