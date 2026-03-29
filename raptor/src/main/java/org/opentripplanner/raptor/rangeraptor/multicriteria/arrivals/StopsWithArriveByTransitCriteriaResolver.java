package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

/**
 * Resolves the set of stops that require the "arrive-by-transit" Pareto criterion in
 * multi-criteria Raptor. At these stops, an arrival by transit (on-board) must be tracked
 * so that thise are not dominated by on-street arrivals(transfers & access). This is because
 * at these stops arriving on-board will give you an advantige - e.g. you must arrive by transit
 * to continue using a via-connection, transfer or egress.
 *
 * <p>A stop is included in the result when at least one of the following holds:
 * <ol>
 *   <li><b>On-board access:</b> an access path arrives at the stop on-board a transit service
 *       (e.g. flex), meaning you are allowed to continue with a transfer in the next round.
 *       The access should not be dominated by a transfer arrival - since using the
 *       access+transfer might be optimal.
 *       </li>
 *   <li><b>Egress witch start on-street(on foot):</b> an egress path leaving the stop on foot
 *       can only be used if the stop is reached by transit.</li>
 *   <li><b>Via transfer:</b> a via-connection departs from the stop as a transfer. Arriving
 *       at the stop by transit is the only way to use the via-connection; Hence it has an
 *       advantige over arriving by transfer.</li>
 * </ol>
 *
 * For all other stops the Raptor algoritmn with rounds and iteratins take care of this.
 *
 */
public class StopsWithArriveByTransitCriteriaResolver {

  private final TIntSet stops = new TIntHashSet();

  private StopsWithArriveByTransitCriteriaResolver(
    AccessPaths accessPaths,
    EgressPaths egressPaths,
    @Nullable ViaConnections viaConnections
  ) {
    addAllAccessStopsWithAtLeastOneStopArrivalOnBoard(accessPaths);
    addAllEgressStopsUsingStreetToDestination(egressPaths);
    addAllViaConnectionsFromStopsWithTransfer(viaConnections);
  }

  public static TIntSet resolve(
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

  private TIntSet stops() {
    return stops;
  }

  private void addAllAccessStopsWithAtLeastOneStopArrivalOnBoard(AccessPaths accessPaths) {
    stops.addAll(
      accessPaths.arrivedOnBoard().stream().mapToInt(RaptorAccessEgress::stop).toArray()
    );
  }

  private void addAllEgressStopsUsingStreetToDestination(EgressPaths egressPaths) {
    var egressByStop = egressPaths.byStop();
    for (var it = egressByStop.keySet().iterator(); it.hasNext(); ) {
      int stop = it.next();
      var egressList = egressByStop.get(stop);
      if (egressList.stream().anyMatch(e -> e.stopReachedByWalking())) {
        stops.add(stop);
      }
    }
  }

  private void addAllViaConnectionsFromStopsWithTransfer(ViaConnections viaConnections) {
    if (viaConnections == null) {
      return;
    }
    for (var it = viaConnections.byFromStop().iterator(); it.hasNext(); ) {
      it.advance();
      var stop = it.key();
      var viaConnectionsFromStop = it.value();
      if (viaConnectionsFromStop.stream().anyMatch(via -> !via.isSameStop())) {
        stops.add(stop);
      }
    }
  }
}
