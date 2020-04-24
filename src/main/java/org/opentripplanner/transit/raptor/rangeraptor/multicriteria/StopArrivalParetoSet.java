package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListenerComposite;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetWithMarker;

import java.util.List;
import java.util.Map;

/**
 * A pareto optimal set of stop arrivals for a given stop.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class StopArrivalParetoSet<T extends RaptorTripSchedule> extends ParetoSetWithMarker<AbstractStopArrival<T>> {

    /**
     * Use the factory methods in this class to create a new instance.
     */
    StopArrivalParetoSet(ParetoSetEventListener<ArrivalView<T>> listener) {
        super(AbstractStopArrival.compareArrivalTimeRoundAndCost(), listener);
    }

    /**
     * Create a stop arrivals pareto set and attach a debugger is handler exist.
     */
    static <T extends RaptorTripSchedule> StopArrivalParetoSet<T> createStopArrivalSet(
            int stop,
            DebugHandlerFactory<T> debugHandlerFactory
    ) {
        return new StopArrivalParetoSet<>(debugHandlerFactory.paretoSetStopArrivalListener(stop));
    }

    /**
     * Create a new StopArrivalParetoSet and attach a debugger if it exist. Also
     * attach a {@link CalculateTransferToDestination} listener witch will create
     * new destination arrivals for each accepted egress stop arrival.
     */
    static <T extends RaptorTripSchedule> StopArrivalParetoSet<T> createEgressStopArrivalSet(
            Map.Entry<Integer, List<RaptorTransfer>> egressLegs,
            CostCalculator costCalculator,
            DestinationArrivalPaths<T> destinationArrivals,
            DebugHandlerFactory<T> debugHandlerFactory
    ) {
        ParetoSetEventListener<ArrivalView<T>> listener;
        ParetoSetEventListener<ArrivalView<T>> debugListener;

        listener = new CalculateTransferToDestination<>(egressLegs.getValue(), destinationArrivals, costCalculator);
        debugListener = debugHandlerFactory.paretoSetStopArrivalListener(egressLegs.getKey());

        if(debugListener != null) {
            listener = new ParetoSetEventListenerComposite<>(debugListener, listener);
        }

        return new StopArrivalParetoSet<>(listener);
    }
}
