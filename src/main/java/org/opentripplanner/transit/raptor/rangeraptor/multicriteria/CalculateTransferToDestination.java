package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import java.util.List;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListener;

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

    CalculateTransferToDestination(
            List<RaptorTransfer> egressPaths,
            DestinationArrivalPaths<T> destinationArrivals
    ) {
        this.egressPaths = egressPaths;
        this.destinationArrivals = destinationArrivals;
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
        if(newElement.arrivedByTransit()) {
            for (RaptorTransfer egress : egressPaths) {
                destinationArrivals.add(newElement, egress);
            }
        }
        else if (newElement.arrivedByTransfer()) {
            for (RaptorTransfer egress : egressPaths) {
                if (egress.stopReachedOnBoard()) {
                    destinationArrivals.add(newElement, egress);
                }
            }
        }
    }
}
