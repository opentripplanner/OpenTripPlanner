package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.util.OTPFeature;


/**
 * The responsibility of this class is to provide transfer constraints to the Raptor search
 * for a given pattern. The instance is stateful and not thread-safe. The current stop
 * position is checked for transfers, then the provider is asked to list all transfers
 * between the current pattern and the source trip stop arrival. The source is the "from"
 * point in a transfer for a forward search, and the "to" point in the reverse search.
 */
public final class ConstrainedBoardingSearch
        implements RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> {

    /**
     * Abort the search after looking at 5 valid boardings. In the case where this happens, one of
     * these trips are probably a better match. We abort to avoid stepping through all trips,
     * possibly a large number (several days).
     */
    private static final int ABORT_SEARCH_AFTER_N_VAILD_NORMAL_TRIPS = 5;

    private static final ConstrainedBoardingSearchStrategy FORWARD_STRATEGY = new ConstrainedBoardingSearchForward();
    private static final ConstrainedBoardingSearchStrategy REVERSE_STRATEGY = new ConstrainedBoardingSearchReverse();

    /** Handle forward and reverse specific tasks */
    private final ConstrainedBoardingSearchStrategy searchStrategy;

    /**
     * List of transfers for each stop position in pattern
     */
    private final TransferForPatternByStopPos transfers;

    private List<TransferForPattern> currentTransfers;
    private int currentTargetStopPos;

    // If we find a trip these variables are used to cache the result
    private int onTripEarliestBoardTime;
    private int onTripIndex;
    private TransferConstraint onTripTxConstraint;

    public ConstrainedBoardingSearch(
            boolean forwardSearch,
            TransferForPatternByStopPos transfers
    ) {
        this.transfers = transfers;
        this.searchStrategy = forwardSearch ? FORWARD_STRATEGY : REVERSE_STRATEGY;
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
            int prevTransitArrivalTime,
            int earliestBoardTime
    ) {
        var transfers = findMatchingTransfers(sourceTripSchedule, sourceStopIndex);

        if(transfers.isEmpty()) { return null; }

        boolean found = findTimetableTripInfo(
                timetable,
                transfers,
                currentTargetStopPos,
                prevTransitArrivalTime,
                earliestBoardTime
        );

        if(!found) { return null; }

        var trip = timetable.getTripSchedule(onTripIndex);
        int departureTime = searchStrategy.time(trip, currentTargetStopPos);

        return new ConstrainedTransferBoarding<>(
                onTripTxConstraint,
                onTripIndex,
                trip,
                currentTargetStopPos,
                departureTime,
                onTripEarliestBoardTime
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
     * Find the trip to board (trip index) and the transfer constraint.
     * <p>
     * This method sets the following parameters if successful:
     * <ul>
     *     <li>{@code onTripIndex}
     *     <li>{@code onTripTxConstraint}
     *     <li>{@code onTripEarliestBoardTime}
     * </ul>
     *
     * @return {@code true} if a matching trip is found
     */
    public boolean findTimetableTripInfo(
            RaptorTimeTable<TripSchedule> timetable,
            List<TransferForPattern> transfers,
            int stopPos,
            int sourceTransitArrivalTime,
            int earliestBoardTime
    ) {
        int nAllowedBoardings = 0;
        boolean useNextNormalTrip = false;

        var index = searchStrategy.scheduleIndexIterator(timetable);
        outer:
        while (index.hasNext()) {
            onTripIndex = index.next();
            var it = timetable.getTripSchedule(onTripIndex);

            // Forward: boardTime, Reverse: alightTime
            int time = searchStrategy.time(it, stopPos);

            if (searchStrategy.timeIsBefore(time, sourceTransitArrivalTime)) { continue; }

            ++nAllowedBoardings;

            var targetTrip = it.getOriginalTripTimes().getTrip();

            for (TransferForPattern tx : transfers) {
                onTripTxConstraint = (TransferConstraint)tx.getTransferConstraint();

                if(onTripTxConstraint.isFacilitated()) {
                    onTripEarliestBoardTime = sourceTransitArrivalTime;
                }
                // If NOT guaranteed or stay-seated the boarding is only allowed if there is
                // enough time to do the transfer
                else {
                    if(onTripTxConstraint.isMinTransferTimeSet()) {
                        int minTransferBoardTime = searchStrategy.plus(
                                sourceTransitArrivalTime,
                                onTripTxConstraint.getMinTransferTime()
                        );
                        // If this feature flag is switched on, then the minimum transfer time is
                        // not the minimum transfer time, but the definitive transfer time. Use
                        // this to override what we think the transfer will take according to OSM
                        // data, for example if you want to set a very low transfer time like
                        // 1 minute.
                        if (OTPFeature.MinimumTransferTimeIsDefinitive.isOn()) {
                            onTripEarliestBoardTime = minTransferBoardTime;
                        }
                        // Here we take the max(minTransferTime, osmWalkTransferTime) as the
                        // transfer time. If the minimum transfer time is 5 minutes, but according
                        // to OSM data it will take 7 minutes to walk then 7 minutes is chosen.
                        else {
                            onTripEarliestBoardTime = searchStrategy.maxTime(
                                    earliestBoardTime,
                                    minTransferBoardTime
                            );
                        }

                    } else {
                        onTripEarliestBoardTime = earliestBoardTime;
                    }

                    if (searchStrategy.timeIsBefore(time, onTripEarliestBoardTime)) { continue; }
                }


                if (tx.applyToAllTargetTrips()) {
                    return true;
                }
                else if (tx.applyToTargetTrip(targetTrip)) {
                    if (onTripTxConstraint.isNotAllowed()) {
                        useNextNormalTrip = true;
                        continue outer;
                    }
                    else {
                        return true;
                    }
                }
            }
            if (useNextNormalTrip) {
                onTripEarliestBoardTime = earliestBoardTime;
                onTripTxConstraint = TransferConstraint.REGULAR_TRANSFER;
                return true;
            }
            if(nAllowedBoardings == ABORT_SEARCH_AFTER_N_VAILD_NORMAL_TRIPS) {
                return false;
            }
        }
        return false;
    }
}
