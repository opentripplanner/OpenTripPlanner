package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

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
