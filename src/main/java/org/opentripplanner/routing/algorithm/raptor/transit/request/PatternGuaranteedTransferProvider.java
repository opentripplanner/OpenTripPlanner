package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.List;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorGuaranteedTransferProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.util.AvgTimer;

/**
 *
 * <h3>Implementation notes</h3>
 * This is implemented as a class. It might make sense to use an interface instead,
 * but it is overkill for now.
 */
final class PatternGuaranteedTransferProvider
        implements RaptorGuaranteedTransferProvider<TripSchedule> {

    private static final AvgTimer LOOKUP_GUARANTEED_TRANSFER_TIMER = AvgTimer.timerMicroSec("lookupGuaranteedTransfer");

    private final DirectionHelper translator;
    private final RaptorRoute<TripSchedule> targetRoute;

    // TODO GuaranteedTransfer
    private final TIntObjectMap<List<Transfer>> transfers;

    private List<Transfer> currentTransfers;

    PatternGuaranteedTransferProvider(
            boolean forwardSearch,
            RaptorRoute<TripSchedule> targetRoute,
            TIntObjectMap<List<Transfer>> transfers
    ) {
        this.translator = forwardSearch
                ? new ForwardDirectionHelper()
                : new ReverseDirectionHelper();
        this.targetRoute = targetRoute;
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
            TripSchedule sourceTripSchedule,
            int sourceStopIndex,
            int sourceArrivalTime
    ) {
        return LOOKUP_GUARANTEED_TRANSFER_TIMER.timeAndReturn(() -> {
            final Trip sourceTrip = sourceTripSchedule.getOriginalTripTimes().trip;
            final int sourceStopPos = translator.findSourceStopPosition(
                    sourceTripSchedule, sourceArrivalTime, sourceStopIndex
            );

            TransferPoint targetPoint = findMatchingTargetPoint(sourceTrip, sourceStopPos);

            if(targetPoint == null) { return null; }

            return translator.findTimetableTripIndex(
                    targetRoute.timetable(),
                    targetPoint.getTrip(),
                    targetPoint.getStopPosition(),
                    sourceArrivalTime
            );
        });
    }

    private TransferPoint findMatchingTargetPoint(
            Trip sourceTrip,
            int sourceStopPos
    ) {
    for (Transfer tx : currentTransfers) {
            var sourcePoint = translator.source(tx);
            if(sourcePoint.matches(sourceTrip, sourceStopPos)) {
                return translator.target(tx);
            }
        }
        return null;
    }

    private interface DirectionHelper {
        TransferPoint source(Transfer tx);
        TransferPoint target(Transfer tx);
        int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop);
        Result findTimetableTripIndex(
                RaptorTimeTable<TripSchedule> timetable, Trip trip, int stopPos, int sourceTime
        );
    }

    private static class ForwardDirectionHelper implements DirectionHelper {
        @Override public TransferPoint source(Transfer tx) { return tx.getFrom();  }
        @Override public TransferPoint target(Transfer tx) { return tx.getTo(); }
        @Override
        public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
            return schedule.findArrivalStopPosition(timeLimit, stop);
        }
        @Override
        public Result findTimetableTripIndex(
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
                if(departureTime > maxLimit) { return null; }
                if(it.getOriginalTripTimes().trip == trip) {
                    return new Result(i, it, stopPos, departureTime);
                }
            }
            return null;
        }
    }

    private static class ReverseDirectionHelper implements DirectionHelper {
        @Override public TransferPoint source(Transfer tx) { return tx.getTo();  }
        @Override public TransferPoint target(Transfer tx) { return tx.getFrom(); }
        @Override
        public int findSourceStopPosition(RaptorTripSchedule schedule, int timeLimit, int stop) {
            return schedule.findDepartureStopPosition(timeLimit, stop);
        }
        @Override
        public Result findTimetableTripIndex(
                RaptorTimeTable<TripSchedule> timetable,
                Trip trip,
                int stopPos,
                int toDepartureTime
        ) {
            // Abort after 6 hours
            int minLimit = toDepartureTime - 3600 * 6;

            for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
                var it = timetable.getTripSchedule(i);
                int arrivalTime = it.arrival(stopPos);
                if(arrivalTime < minLimit) { continue; }
                if(arrivalTime > toDepartureTime) { return null; }
                if(it.getOriginalTripTimes().trip == trip) {
                    return new Result(i, it, stopPos, arrivalTime);
                }
            }
            return null;
        }
    }

    private static class Result implements RaptorTripScheduleBoardOrAlightEvent<TripSchedule> {
        private final int tripIndex;
        private final TripSchedule trip;
        private final int stopPositionInPattern;
        private final int time;

        private Result(int tripIndex, TripSchedule trip, int stopPositionInPattern, int time) {
            this.tripIndex = tripIndex;
            this.trip = trip;
            this.stopPositionInPattern = stopPositionInPattern;
            this.time = time;
        }

        @Override public int getTripIndex() { return tripIndex; }
        @Override public TripSchedule getTrip() { return trip; }
        @Override public int getStopPositionInPattern() { return stopPositionInPattern; }
        @Override public int getTime() { return time; }
    }
}
