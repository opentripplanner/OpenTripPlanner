package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListenerComposite;
import org.opentripplanner.raptor.util.paretoset.ParetoSetWithMarker;

/**
 * A pareto optimal set of stop arrivals for a given stop.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class StopArrivalParetoSet<T extends RaptorTripSchedule>
  extends ParetoSetWithMarker<McStopArrival<T>> {

  /**
   * Use the factory methods in this class to create a new instance.
   */
  private StopArrivalParetoSet(
    ParetoComparator<McStopArrival<T>> comparator,
    ParetoSetEventListener<ArrivalView<T>> listener
  ) {
    super(comparator, listener);
  }

  public static <T extends RaptorTripSchedule> Builder<T> of(
    ParetoComparator<McStopArrival<T>> comparator
  ) {
    return new Builder<>(comparator);
  }

  static class Builder<T extends RaptorTripSchedule> {

    private ParetoSetEventListener<ArrivalView<T>> debugListener = null;
    private ParetoSetEventListener<ArrivalView<T>> egressListener = null;
    private ParetoSetEventListener<ArrivalView<T>> nextSearchListener = null;
    private final ParetoComparator<McStopArrival<T>> comparator;

    Builder(ParetoComparator<McStopArrival<T>> comparator) {
      this.comparator = comparator;
    }

    /**
     * Attach an optional debug handler.
     */
    Builder<T> withDebugListener(ParetoSetEventListener<ArrivalView<T>> debugListener) {
      this.debugListener = debugListener;
      return this;
    }

    /**
     * Attach a {@link CalculateTransferToDestination} listener which will create new destination
     * arrivals for each accepted egress stop arrival.
     */
    Builder<T> withEgressListener(
      List<RaptorAccessEgress> egressPaths,
      DestinationArrivalPaths<T> destinationArrivals
    ) {
      this.egressListener = new CalculateTransferToDestination<>(egressPaths, destinationArrivals);
      return this;
    }

    /**
     * Attach an optional listener for copy state over to the next-leg Raptor search.
     */
    Builder<T> withNextLegListener(ParetoSetEventListener<ArrivalView<T>> nextSearchListener) {
      this.nextSearchListener = nextSearchListener;
      return this;
    }

    StopArrivalParetoSet<T> build() {
      // The order of the listeners is important, we want the debug event for reaching a
      // stop to appear before the path is logged (in case both debuggers are enabled).
      return new StopArrivalParetoSet<>(
        comparator,
        ParetoSetEventListenerComposite.of(debugListener, nextSearchListener, egressListener)
      );
    }
  }
}
