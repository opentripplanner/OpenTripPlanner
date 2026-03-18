package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListenerComposite;

/// This class creates {@link ParetoSetEventListener}s to glue a stop-arrival to (in order):
/// - debug stop arrival events
/// - updating next connection event
/// - updating destination path collection using egress paths.
///
/// If the debugger is not done first, then the logging will be strange -> events
/// arriving at the destination, before arriving at the egress stop.
public class McArrivalsEventListenerFactory<T extends RaptorTripSchedule> {

  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener;
  private final @Nullable EgressPaths egressPaths;
  private final DestinationArrivalPaths<T> destinationPaths;
  private boolean processed = false;

  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners =
    new TIntObjectHashMap<>();

  public McArrivalsEventListenerFactory(
    DebugHandlerFactory<T> debugHandlerFactory,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener,
    @Nullable EgressPaths egressPaths,
    DestinationArrivalPaths<T> destinationPaths
  ) {
    this.debugHandlerFactory = debugHandlerFactory;
    this.nextConnectionListener = nextConnectionListener;
    this.egressPaths = egressPaths;
    this.destinationPaths = destinationPaths;
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals are added to the destination arrivals.
   */
  public McArrivalsEventListenerFactory<T> create() {
    for (int stop : nextConnectionListener.keys()) {
      append(stop, nextConnectionListener.get(stop));
    }
    var egressByStop = egressPaths.byStop();

    for (int stop : egressByStop.keys()) {
      List<RaptorAccessEgress> egressList = egressByStop.get(stop);
      append(stop, new CalculateTransferToDestination<>(egressList, destinationPaths));
    }
    processed = true;
    return this;
  }

  public TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners() {
    return assertProcessed(arrivalListeners);
  }

  private void append(int stop, ParetoSetEventListener<ArrivalView<T>> listener) {
    var e = arrivalListeners.get(stop);
    var l = listener;
    if (e != null) {
      // existing listeners should be inserted before new one
      l = ParetoSetEventListenerComposite.of(e, listener);
    } else if (debugHandlerFactory.isDebugStopArrival(stop)) {
      // Debug listerner is inserted first
      var debug = debugHandlerFactory.paretoSetStopArrivalListener(stop);
      l = ParetoSetEventListenerComposite.of(debug, l);
    }
    arrivalListeners.put(stop, l);
  }

  private <R> R assertProcessed(R result) {
    if (!processed) {
      throw new IllegalStateException("Call the map() method before accessing the result!");
    }
    return result;
  }
}
