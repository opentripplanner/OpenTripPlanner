package org.opentripplanner.raptor.rangeraptor.transit;

import static java.util.stream.Collectors.groupingBy;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.request.via.ViaConnection;

public class ViaConnections {

  private final TIntObjectMap<List<ViaConnection>> groupByFromStop;

  public ViaConnections(Collection<ViaConnection> viaConnections) {
    this.groupByFromStop = new TIntObjectHashMap<>();
    viaConnections
      .stream()
      .collect(groupingBy(ViaConnection::fromStop))
      .forEach(groupByFromStop::put);
  }

  public TIntObjectMap<List<ViaConnection>> byFromStop() {
    return groupByFromStop;
  }
}
