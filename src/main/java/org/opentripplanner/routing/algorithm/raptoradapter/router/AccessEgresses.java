package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;

class AccessEgresses {

  private final Collection<DefaultAccessEgress> accesses;
  private final Collection<DefaultAccessEgress> egresses;

  AccessEgresses(
    Collection<DefaultAccessEgress> accesses,
    Collection<DefaultAccessEgress> egresses
  ) {
    this.accesses = accesses;
    this.egresses = egresses;
  }

  Collection<DefaultAccessEgress> getAccesses() {
    return accesses;
  }

  Collection<DefaultAccessEgress> getEgresses() {
    return egresses;
  }
}
