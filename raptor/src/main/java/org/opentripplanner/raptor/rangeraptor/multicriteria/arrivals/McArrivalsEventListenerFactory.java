package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListenerComposite;

/// This class creates {@link ParetoSetEventListener}s to glue a stop-arrival to (in order):
/// - debug stop arrival events
/// - updating next via connection event
/// - updating destination path collection using egress paths.
///
/// If the debugger is not done first, then the logging will be strange -> events
/// arriving at the destination, before arriving at the egress stop.
public class McArrivalsEventListenerFactory<T extends RaptorTripSchedule> {

  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener;
  private final EgressPaths egressPaths;
  private final DestinationArrivalPaths<T> destinationPaths;
  private boolean processed = false;

  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners =
    new TIntObjectHashMap<>();

  public McArrivalsEventListenerFactory(
    DebugHandlerFactory<T> debugHandlerFactory,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener,
    EgressPaths egressPaths,
    DestinationArrivalPaths<T> destinationPaths
  ) {
    this.debugHandlerFactory = debugHandlerFactory;
    this.nextConnectionListener = nextConnectionListener;
    this.egressPaths = egressPaths;
    this.destinationPaths = destinationPaths;
  }

  public TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners() {
    return assertProcessed(arrivalListeners);
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals are added to the destination arrivals.
   */
  public McArrivalsEventListenerFactory<T> create() {
    var egressByStop = egressPaths.byStop();

    TIntSet stops = new TIntHashSet();
    stops.addAll(nextConnectionListener.keySet());
    stops.addAll(egressByStop.keySet());

    var it = stops.iterator();
    while (it.hasNext()) {
      createListenerForStop(it.next(), egressByStop);
    }

    this.processed = true;
    return this;
  }

  private void createListenerForStop(
    int stop,
    TIntObjectMap<List<RaptorAccessEgress>> egressByStop
  ) {
    var l = ParetoSetEventListenerComposite.of(
      debugHandlerFactory.paretoSetStopArrivalListener(stop),
      egressStopListeners(egressByStop.get(stop)),
      nextConnectionListener.get(stop)
    );
    this.arrivalListeners.put(stop, l);
  }

  private ParetoSetEventListener<ArrivalView<T>> egressStopListeners(
    List<RaptorAccessEgress> egressList
  ) {
    return egressList == null
      ? null
      : new CalculateTransferToDestination<>(egressList, destinationPaths);
  }

  private <R> R assertProcessed(R result) {
    if (!processed) {
      throw new IllegalStateException("Call the create() method before accessing the result!");
    }
    return result;
  }
}
