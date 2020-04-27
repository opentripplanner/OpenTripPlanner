package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;


/**
 * The purpose of this class is hold information about a destination arrival and
 * compute the values for arrival time and cost.
 * <p/>
 * Compared with the ParetoSet of each stop we need two extra criteria:
 * <ul>
 * <li>Number of transfers. The McRangeRaptor works in rounds, so
 * there is no need to include rounds in the intermediate stop pareto sets.
 * But to avoid that a later iteration delete an earlier result with less
 * transfers, transfers need to be added as a criterion to the final destination.
 *
 * <li>Travel time duration - Range Raptor works in iteration. So when a
 * later iteration makes it into the destination set - it should not erase
 * an earlier result unless it is faster. There is no check on total travel
 * duration for each stop, because it does not need to.
 *
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class DestinationArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

    private final ArrivalView<T> previous;
    private final RaptorTransfer egress;
    private final int departureTime;
    private final int arrivalTime;
    private final int numberOfTransfers;
    private final int cost;


    DestinationArrival(RaptorTransfer egress, ArrivalView<T> previous, int departureTime, int arrivalTime, int additionalCost) {
        this.previous = previous;
        this.egress = egress;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.numberOfTransfers = previous.round() - 1;
        this.cost = previous.cost() + additionalCost;
    }

    int numberOfTransfers() {
        return numberOfTransfers;
    }

    @Override
    public int stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RaptorTransfer accessEgress() {
        return egress;
    }

    @Override
    public int round() {
        return numberOfTransfers + 1;
    }

    @Override
    public int departureTime() {
        return departureTime;
    }

    @Override
    public int arrivalTime() {
        return arrivalTime;
    }

    @Override
    public int cost() {
        return cost;
    }

    @Override
    public ArrivalView<T> previous() {
        return previous;
    }

    @Override
    public boolean arrivedAtDestination() {
        return true;
    }

    @Override
    public int transferFromStop() {
        return previous == null ? -1 : previous.stop();
    }

    @Override
    public String toString() {
        return asString();
    }
}
