package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

public class StopsWithArriveByTransitCriteriaResolver<T extends RaptorTripSchedule> {

  private final AccessPaths accessPaths;
  private final EgressPaths egressPaths;
  private final @Nullable ViaConnections viaConnections;

  private final TIntSet stops = new TIntHashSet();

  public StopsWithArriveByTransitCriteriaResolver(
    AccessPaths accessPaths,
    EgressPaths egressPaths,
    @Nullable ViaConnections viaConnections
  ) {
    this.accessPaths = accessPaths;
    this.egressPaths = egressPaths;
    this.viaConnections = viaConnections;
  }

  public TIntSet stops() {
    if (stops.isEmpty()) {
      create();
    }
    return stops;
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals are added to the destination arrivals.
   */
  private StopsWithArriveByTransitCriteriaResolver<T> create() {
    addAllAccessStopsWithAtLeastOneStopArrivalOnBoard();
    addAllEgressStopsUsingStreetToDestination();
    addAllViaConnectionsFromStopsWithTransfer();

    return this;
  }

  private void addAllAccessStopsWithAtLeastOneStopArrivalOnBoard() {
    stops.addAll(
      accessPaths.arrivedOnBoard().stream().mapToInt(RaptorAccessEgress::stop).toArray()
    );
  }

  private void addAllEgressStopsUsingStreetToDestination() {
    var egressByStop = egressPaths.byStop();
    for (var it = egressByStop.keySet().iterator(); it.hasNext(); ) {
      int stop = it.next();
      var egressList = egressByStop.get(stop);
      if (egressList.stream().anyMatch(e -> !e.isFree())) {
        stops.add(stop);
      }
    }
  }

  private void addAllViaConnectionsFromStopsWithTransfer() {
    if (viaConnections == null) {
      return;
    }
    for (var it = viaConnections.byFromStop().iterator(); it.hasNext(); ) {
      it.advance();
      var stop = it.key();
      var viaConnections = it.value();
      if (viaConnections.stream().anyMatch(via -> !via.isSameStop())) {
        stops.add(stop);
      }
    }
  }
}
