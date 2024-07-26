package org.opentripplanner.raptor.rangeraptor.transit;

import static java.util.stream.Collectors.groupingBy;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.request.ViaConnection;

public class ViaConnections {

  private final TIntObjectMap<List<ViaConnection>> byFromStop;

  public ViaConnections(Collection<ViaConnection> viaConnections) {
    this.byFromStop = new TIntObjectHashMap<>();
    viaConnections.stream().collect(groupingBy(ViaConnection::fromStop)).forEach(byFromStop::put);
  }

  public TIntObjectMap<List<ViaConnection>> byFromStop() {
    return byFromStop;
  }
}
