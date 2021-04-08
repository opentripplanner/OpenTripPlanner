package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.TransferStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.TransitStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListener;

import java.util.List;

/**
 * This class listen to pareto set egress stop arrivals and on accepted
 * transit arrivals make the transfer to the destination.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class CalculateTransferToDestination<T extends RaptorTripSchedule>
        implements ParetoSetEventListener<ArrivalView<T>> {

    private final List<RaptorTransfer> egressPaths;
    private final DestinationArrivalPaths<T> destinationArrivals;
    private final CostCalculator<T> costCalculator;

    CalculateTransferToDestination(
            List<RaptorTransfer> egressPaths,
            DestinationArrivalPaths<T> destinationArrivals,
            CostCalculator<T> costCalculator
    ) {
        this.egressPaths = egressPaths;
        this.destinationArrivals = destinationArrivals;
        this.costCalculator = costCalculator;
    }

    /**
     * When a stop arrival is accepted and we arrived by transit, then add a new destination arrival.
     * <p/>
     * We do not have to handle other events like dropped or rejected.
     *
     * @param newElement the new transit arrival
     */
    @Override
    public void notifyElementAccepted(ArrivalView<T> newElement) {
        if(newElement instanceof TransitStopArrival) {
            TransitStopArrival<T> transitStopArrival = (TransitStopArrival<T>) newElement;
            for (RaptorTransfer egress : egressPaths) {
                destinationArrivals.add(
                    transitStopArrival,
                    egress,
                    costCalculator.walkCost(egress.durationInSeconds())
                );
            }
        } else if (newElement instanceof TransferStopArrival) {
            for (RaptorTransfer egress : egressPaths) {
                if (egress.stopReachedOnBoard()) {
                    TransferStopArrival<T> transferStopArrival = (TransferStopArrival<T>) newElement;
                    destinationArrivals.add(
                        transferStopArrival,
                        egress,
                        costCalculator.walkCost(egress.durationInSeconds())
                    );
                }
            }
        }
    }
}
