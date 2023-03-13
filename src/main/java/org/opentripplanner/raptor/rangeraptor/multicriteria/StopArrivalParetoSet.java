package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.List;
import javax.annotation.Nullable;
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

  /**
   * Create a stop arrivals pareto set and attach an optional {@code paretoSetEventListener}
   * (debug handler).
   * <p>
   * The comparator used compare:
   * <ul>
   *   <li>arrival-time</li>
   *   <li>round</li>
   *   <li>generalized-cost</li>
   *   <li><b>But not on-board-arrival</b></li>
   * </ul>
   */
  static <T extends RaptorTripSchedule> StopArrivalParetoSet<T> createStopArrivalSet(
    @Nullable ParetoSetEventListener<ArrivalView<T>> paretoSetEventListener
  ) {
    return new StopArrivalParetoSet<>(
      McStopArrival.compareArrivalTimeRoundAndCost(),
      paretoSetEventListener
    );
  }

  /**
   * Create a stop arrivals pareto set and attach an optional {@code paretoSetEventListener}
   * (debug handler).
   * <p>
   * The comparator used compare:
   * <ul>
   *   <li>arrival-time</li>
   *   <li>round</li>
   *   <li>generalized-cost</li>
   *   <li>on-board-arrival</li>
   * </ul>
   */
  static <
    T extends RaptorTripSchedule
  > StopArrivalParetoSet<T> createStopArrivalSetWithOnBoardCriteria(
    @Nullable ParetoSetEventListener<ArrivalView<T>> paretoSetEventListener
  ) {
    return new StopArrivalParetoSet<>(
      McStopArrival.compareArrivalTimeRoundCostAndOnBoardArrival(),
      paretoSetEventListener
    );
  }

  /**
   * Create a new StopArrivalParetoSet and attach a debugger if it exist. Also attach a {@link
   * CalculateTransferToDestination} listener which will create new destination arrivals for each
   * accepted egress stop arrival.
   */
  static <T extends RaptorTripSchedule> StopArrivalParetoSet<T> createEgressStopArrivalSet(
    List<RaptorAccessEgress> egressPaths,
    DestinationArrivalPaths<T> destinationArrivals,
    @Nullable ParetoSetEventListener<ArrivalView<T>> paretoSetEventListener
  ) {
    ParetoSetEventListener<ArrivalView<T>> listener;

    listener = new CalculateTransferToDestination<>(egressPaths, destinationArrivals);

    if (paretoSetEventListener != null) {
      listener = new ParetoSetEventListenerComposite<>(paretoSetEventListener, listener);
    }

    return new StopArrivalParetoSet<>(
      McStopArrival.compareArrivalTimeRoundCostAndOnBoardArrival(),
      listener
    );
  }
}
