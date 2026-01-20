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
public class ArrivalsEventListenerMapper<T extends RaptorTripSchedule> {

  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener;
  private final @Nullable EgressPaths egressPaths;
  private final DestinationArrivalPaths<T> paths;

  private final TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> result =
    new TIntObjectHashMap<>();

  private ArrivalsEventListenerMapper(
    DebugHandlerFactory<T> debugHandlerFactory,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> nextConnectionListener,
    @Nullable EgressPaths egressPaths,
    DestinationArrivalPaths<T> paths
  ) {
    this.debugHandlerFactory = debugHandlerFactory;
    this.nextConnectionListener = nextConnectionListener;
    this.egressPaths = egressPaths;
    this.paths = paths;
  }

  public static <T extends RaptorTripSchedule> TIntObjectMap<
    ParetoSetEventListener<ArrivalView<T>>
  > map(
    DebugHandlerFactory<T> debugHandlerFactory,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> connectionListeners,
    EgressPaths egressPaths,
    DestinationArrivalPaths<T> destinationPaths
  ) {
    return new ArrivalsEventListenerMapper<>(
      debugHandlerFactory,
      connectionListeners,
      egressPaths,
      destinationPaths
    ).map();
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals are added to the destination arrivals.
   */
  private TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> map() {
    for (int stop : nextConnectionListener.keys()) {
      append(stop, nextConnectionListener.get(stop));
    }
    var egressByStop = egressPaths.byStop();

    for (int stop : egressByStop.keys()) {
      List<RaptorAccessEgress> egressList = egressByStop.get(stop);
      append(stop, new CalculateTransferToDestination<>(egressList, paths));
    }
    return result;
  }

  private void append(int stop, ParetoSetEventListener<ArrivalView<T>> listener) {
    var e = result.get(stop);
    var l = listener;
    if (e != null) {
      // existing listeners should be inserted before new one
      l = ParetoSetEventListenerComposite.of(e, listener);
    } else if (debugHandlerFactory.isDebugStopArrival(stop)) {
      // Debug listerner is inserted first
      var debug = debugHandlerFactory.paretoSetStopArrivalListener(stop);
      l = ParetoSetEventListenerComposite.of(debug, l);
    }
    result.put(stop, l);
  }
}
