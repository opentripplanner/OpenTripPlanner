package org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
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

    private static final ConstrainedBoardingSearchStrategy FORWARD_STRATEGY = new ConstrainedBoardingSearchForward();
    private static final ConstrainedBoardingSearchStrategy REVERSE_STRATEGY = new ConstrainedBoardingSearchReverse();

    /** Handle forward and reverse specific tasks */
    private final ConstrainedBoardingSearchStrategy translator;

    /**
     * List of transfers for each stop position in pattern
     */
    private final TransferForPatternByStopPos transfers;

    private List<TransferForPattern> currentTransfers;
    private int currentTargetStopPos;

    public ConstrainedBoardingSearch(
            boolean forwardSearch,
            TransferForPatternByStopPos transfers
    ) {
        this.transfers = transfers;
        this.translator = forwardSearch ? FORWARD_STRATEGY : REVERSE_STRATEGY;
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
        var transfers = findMatchingTransfers(sourceTripSchedule, sourceStopIndex);

        if(transfers.isEmpty()) { return null; }

        T2<Integer, RaptorTransferConstraint> tripInfo = findTimetableTripInfo(
                timetable,
                transfers,
                currentTargetStopPos,
                sourceArrivalTime
        );

        if(tripInfo == null) { return null; }

        final int tripIndex = tripInfo.first;
        final var transferConstraint = tripInfo.second;

        var trip = timetable.getTripSchedule(tripIndex);
        int departureTime = translator.time(trip, currentTargetStopPos);

        return new ConstrainedTransferBoarding<>(
                transferConstraint, tripIndex, trip, currentTargetStopPos, departureTime
        );
    }

    private List<TransferForPattern> findMatchingTransfers(
            TripSchedule tripSchedule,
            int stopIndex
    ) {
        final Trip trip = tripSchedule.getOriginalTripTimes().getTrip();
        return currentTransfers.stream()
                .filter(t -> t.matchesSourcePoint(stopIndex, trip))
                .collect(Collectors.toList());
    }

    /**
     * Find the trip to board (trip index) and the transfer constraint
     */
    public T2<Integer, RaptorTransferConstraint> findTimetableTripInfo(
            RaptorTimeTable<TripSchedule> timetable,
            List<TransferForPattern> transfers,
            int stopPos,
            int sourceTime
    ) {
        // Abort after 6 hours
        boolean useNextNormalTrip = false;

        var index = translator.scheduleIndexIterator(timetable);
        outer:
        while (index.hasNext()) {
            final int i = index.next();
            var it = timetable.getTripSchedule(i);

            // Forward: boardTime, Reverse: alightTime
            int time = translator.time(it, stopPos);

            if (translator.timeIsBefore(time, sourceTime)) { continue; }

            var targetTrip = it.getOriginalTripTimes().getTrip();

            for (TransferForPattern tx : transfers) {
                if (tx.applyToAllTargetTrips()) {
                    return new T2<>(i, tx.getTransferConstraint());
                }
                else if (tx.applyToTargetTrip(targetTrip)) {
                    if (tx.getTransferConstraint().isNotAllowed()) {
                        useNextNormalTrip = true;
                        continue outer;
                    }
                    else {
                        return new T2<>(i, tx.getTransferConstraint());
                    }
                }
            }
            if (useNextNormalTrip) {
                return new T2<>(i, TransferConstraint.REGULAR_TRANSFER);
            }
        }
        return null;
    }
}
