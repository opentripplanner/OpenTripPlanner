package org.opentripplanner.raptor.rangeraptor.transit;

import static java.util.stream.Collectors.groupingBy;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.request.RaptorViaConnection;

public class ViaConnections {

  private final TIntObjectMap<List<RaptorViaConnection>> groupByFromStop;

  public ViaConnections(Collection<RaptorViaConnection> viaConnections) {
    this.groupByFromStop = new TIntObjectHashMap<>();
    viaConnections
      .stream()
      .collect(groupingBy(RaptorViaConnection::fromStop))
      .forEach(groupByFromStop::put);
  }

  public TIntObjectMap<List<RaptorViaConnection>> byFromStop() {
    return groupByFromStop;
  }
}
