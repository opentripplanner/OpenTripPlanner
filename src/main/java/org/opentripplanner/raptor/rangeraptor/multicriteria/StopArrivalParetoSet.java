package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.List;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListenerComposite;
import org.opentripplanner.raptor.util.paretoset.ParetoSetWithMarker;

/**
 * A pareto optimal set of stop arrivals for a given stop.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class StopArrivalParetoSet<T extends RaptorTripSchedule>
  extends ParetoSetWithMarker<AbstractStopArrival<T>> {

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
   * Create a new StopArrivalParetoSet and attach a debugger if it exist. Also attach a {@link
   * CalculateTransferToDestination} listener which will create new destination arrivals for each
   * accepted egress stop arrival.
   */
  static <T extends RaptorTripSchedule> StopArrivalParetoSet<T> createEgressStopArrivalSet(
    int stop,
    List<RaptorAccessEgress> egressPaths,
    DestinationArrivalPaths<T> destinationArrivals,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    ParetoSetEventListener<ArrivalView<T>> listener;
    ParetoSetEventListener<ArrivalView<T>> debugListener;

    listener = new CalculateTransferToDestination<>(egressPaths, destinationArrivals);
    debugListener = debugHandlerFactory.paretoSetStopArrivalListener(stop);

    if (debugListener != null) {
      listener = new ParetoSetEventListenerComposite<>(debugListener, listener);
    }

    return new StopArrivalParetoSet<>(listener);
  }
}
