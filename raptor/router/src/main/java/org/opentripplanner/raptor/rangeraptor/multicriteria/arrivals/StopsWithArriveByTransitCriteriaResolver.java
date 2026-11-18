package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.api.request.via.RaptorTransferViaConnection;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

/**
 * Resolves the set of stops that require the "arrive-by-transit" Pareto criterion in multi-criteria
 * Raptor. At these stops, an arrival by transit (on-board) must be tracked so that these are not
 * dominated by on-street arrivals(transfers & access). This is because at these stops arriving
 * on-board will give you an advantage - e.g. you must arrive by transit to continue using a
 * via-connection, transfer or egress.
 *
 * <p>
 * A stop is included in the result when at least one of the following holds:
 * <ol>
 *   <li><b>On-board access:</b> an access path arrives at the stop on-board a transit service (e.g.
 *       flex), meaning you are allowed to continue with a transfer in the next round. The access should
 *       not be dominated by a transfer arrival - since using the access+transfer might be optimal.</li>
 *   <li><b>Egress which starts on-street(on foot):</b> an egress path leaving the stop on foot can
 *       only be used if the stop is reached by transit.</li>
 *   <li><b>Via transfer:</b> a via-connection departs from the stop as a transfer. Arriving at the
 *       stop by transit is the only way to use the via-connection; Hence it has an advantage over
 *       arriving by transfer.</li>
 * </ol>
 *
 * For all other stops the Raptor algorithm with rounds and iterations take care of this.
 *
 */
public class StopsWithArriveByTransitCriteriaResolver {

  private final IntSet stops = new IntHashSet();

  private StopsWithArriveByTransitCriteriaResolver(
    AccessPaths accessPaths,
    EgressPaths egressPaths,
    @Nullable ViaConnections viaConnections
  ) {
    addAllAccessStopsWithAtLeastOneStopArrivalOnBoard(accessPaths);
    addAllEgressStopsUsingStreetToDestination(egressPaths);
    addAllViaConnectionsFromStopsWithTransfer(viaConnections);
  }

  public static IntSet resolve(
    AccessPaths accessPaths,
    EgressPaths egressPaths,
    @Nullable ViaConnections viaConnections
  ) {
    return new StopsWithArriveByTransitCriteriaResolver(
      accessPaths,
      egressPaths,
      viaConnections
    ).stops();
  }

  private IntSet stops() {
    return stops;
  }

  private void addAllAccessStopsWithAtLeastOneStopArrivalOnBoard(AccessPaths accessPaths) {
    for (var access : accessPaths.arrivedOnBoard()) {
      stops.add(access.stop());
    }
  }

  private void addAllEgressStopsUsingStreetToDestination(EgressPaths egressPaths) {
    var egressByStop = egressPaths.byStop();
    for (var cursor : egressByStop.keys()) {
      int stop = cursor.value;
      var egressList = egressByStop.get(stop);
      if (egressList.stream().anyMatch(e -> e.arrivedOnStreet())) {
        stops.add(stop);
      }
    }
  }

  private void addAllViaConnectionsFromStopsWithTransfer(ViaConnections viaConnections) {
    if (viaConnections == null) {
      return;
    }

    for (var cursor : viaConnections.byFromStop()) {
      int stop = cursor.key;
      var viaConnectionsFromStop = cursor.value;
      if (
        viaConnectionsFromStop.stream().anyMatch(via -> via instanceof RaptorTransferViaConnection)
      ) {
        stops.add(stop);
      }
    }
  }
}
