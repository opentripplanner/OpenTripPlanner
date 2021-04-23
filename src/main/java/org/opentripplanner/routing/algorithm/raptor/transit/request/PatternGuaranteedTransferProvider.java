package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.List;
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

    private static final AvgTimer lookupTripIndex = AvgTimer.timerMicroSec("b_lookupTripIndex");

    private final boolean forwardSearch;
    private final RaptorRoute<TripSchedule> targetRoute;
    private final TIntObjectMap<List<GuaranteedTransfer<TripSchedule>>> transfers;

    private List<GuaranteedTransfer<TripSchedule>> currentTransfers;
    private int targetStopPos;

    PatternGuaranteedTransferProvider(
            boolean forwardSearch,
            RaptorRoute<TripSchedule> targetRoute,
            TIntObjectMap<List<GuaranteedTransfer<TripSchedule>>> transfers
    ) {
        this.forwardSearch = forwardSearch;
        this.targetRoute = targetRoute;
        this.transfers = transfers;
    }

    @Override
    public final boolean transferExist(int targetStopPos) {
        if(transfers == null) { return false; }

        this.targetStopPos = targetStopPos;
        // Get all guaranteed transfers for the target pattern at the target stop position
        this.currentTransfers = transfers.get(targetStopPos);
        return currentTransfers != null;
    }

    @Override
    public final RaptorTripScheduleBoardOrAlightEvent<TripSchedule> find(
            TripSchedule sourceTrip,
            int sourceStopIndex,
            int sourceArrivalTime
    ) {
        // Search for the targetTrip to board

        TripSchedule targetTrip = forwardSearch
                ? findToTrip(sourceTrip, sourceStopIndex, sourceArrivalTime)
                : findFromTrip(sourceTrip, sourceStopIndex, sourceArrivalTime);

        if(targetTrip == null) { return null; }

        lookupTripIndex.start();
        int tripIndex = findTripIndex(targetRoute.timetable(), targetTrip);
        lookupTripIndex.stop();

        if(tripIndex < 0) { return null; }

        return new TripScheduleBoardOrAlightEvent(targetStopPos, tripIndex, targetTrip);
    }

    private TripSchedule findToTrip(
            TripSchedule fromTrip,
            int fromStopIndex,
            int fromArrivalTime
    ) {
        for (GuaranteedTransfer<TripSchedule> tx : currentTransfers) {
            int stopPos = fromTrip.findArrivalStopPosition(fromArrivalTime, fromStopIndex);
            if(tx.matchesFrom(fromTrip, stopPos)) {
                TripSchedule toTrip = tx.getToTrip();
                return toTrip.departure(targetStopPos) >= fromArrivalTime ? toTrip : null;
            }
        }
        return null;
    }

    private TripSchedule findFromTrip(
            TripSchedule toTrip,
            int toStopIndex,
            int toArrivalTime
    ) {
        for (GuaranteedTransfer<TripSchedule> tx : currentTransfers) {
            int stopPos = toTrip.findDepartureStopPosition(toArrivalTime, toStopIndex);
            if(tx.matchesTo(toTrip, stopPos)) {
                TripSchedule fromTrip = tx.getFromTrip();
                return fromTrip.arrival(targetStopPos) <= toArrivalTime ? fromTrip : null;
            }
        }
        return null;
    }

    /**
     * Find a matching trip in the timetable and return the index to the trip.
     *
     * @param <T> The TripSchedule type defined by the user of the raptor API.
     */
    private static <T extends RaptorTripSchedule> int findTripIndex(RaptorTimeTable<T> timeTable, T trip) {
        // Loop trough the trips to find the right one. This is probably inefficient,
        // but with relatively few guaranteed transfers it is hopefully good enough.
        final int n = timeTable.numberOfTripSchedules();
        for (int i = 0; i < n; i++) {
            T candidate = timeTable.getTripSchedule(i);
            if(trip.equals(candidate)) {
                return i;
            }
        }
        return -1;
    }

}
