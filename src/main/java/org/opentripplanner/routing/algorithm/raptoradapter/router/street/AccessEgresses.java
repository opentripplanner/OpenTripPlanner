package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;

public class AccessEgresses {

  private final Collection<DefaultAccessEgress> accesses;
  private final Collection<DefaultAccessEgress> egresses;

  public AccessEgresses(
    Collection<DefaultAccessEgress> accesses,
    Collection<DefaultAccessEgress> egresses
  ) {
    this.accesses = accesses;
    this.egresses = egresses;
  }

  public Collection<DefaultAccessEgress> getAccesses() {
    return accesses;
  }

  public Collection<DefaultAccessEgress> getEgresses() {
    return egresses;
  }
}
