package org.opentripplanner.raptor.rangeraptor.transit;

import static java.util.stream.Collectors.groupingBy;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.request.via.ViaConnection;

public class ViaConnections {

  private final IntObjectMap<List<ViaConnection>> groupByFromStop;

  public ViaConnections(Collection<ViaConnection> viaConnections) {
    this.groupByFromStop = new IntObjectHashMap<>();
    viaConnections
      .stream()
      .collect(groupingBy(ViaConnection::fromStop))
      .forEach(groupByFromStop::put);
  }

  public IntObjectMap<List<ViaConnection>> byFromStop() {
    return groupByFromStop;
  }
}
