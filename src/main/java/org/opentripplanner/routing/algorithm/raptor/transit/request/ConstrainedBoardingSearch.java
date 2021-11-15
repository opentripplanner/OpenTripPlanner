package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
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

    private final ConstrainedBoardingSearchStrategy translator;

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
        this.translator = forwardSearch ? FORWARD_STRATEGY : REVERSE_STRATEGY;
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
        return currentTransfers.stream()
                .filter(tx -> translator.source(tx).matches(sourceTrip, sourceStopPos))
                .collect(Collectors.toList());
    }
}
