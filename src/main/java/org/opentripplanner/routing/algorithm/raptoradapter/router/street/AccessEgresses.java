package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;

public class AccessEgresses {

  private final Collection<? extends RoutingAccessEgress> accesses;
  private final Collection<? extends RoutingAccessEgress> egresses;

  public AccessEgresses(
    Collection<? extends RoutingAccessEgress> accesses,
    Collection<? extends RoutingAccessEgress> egresses
  ) {
    this.accesses = accesses;
    this.egresses = egresses;
  }

  public Collection<? extends RoutingAccessEgress> getAccesses() {
    return accesses;
  }

  public Collection<? extends RoutingAccessEgress> getEgresses() {
    return egresses;
  }

  public Duration calculateMaxAccessTimePenalty() {
    return accesses
      .stream()
      .map(it -> it.penalty().time())
      .max(Duration::compareTo)
      .orElse(Duration.ZERO);
  }
}
