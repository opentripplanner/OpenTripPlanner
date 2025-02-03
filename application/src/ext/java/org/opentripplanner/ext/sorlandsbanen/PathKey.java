package org.opentripplanner.ext.sorlandsbanen;

import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;

/**
 * The purpose of this class is to create a key to be able to compare paths so duplicate results
 * can be ignored.
 * <p>
 * Creating a good key for a path is not easy. For example, should a small variation in the street
 * routing for an access/egress leg count as a significant difference? The solution here is
 * straightforward. It creates a hash of the access-, egress- and transit-legs in the path,
 * ignoring transfer legs. This approach may drop valid results if there are hash collisions,
 * but since this is a Sandbox module and the investment in this code is minimal, we will accept
 * the risk.
 */
final class PathKey {

  private final int hash;

  PathKey(RaptorPath<?> path) {
    this.hash = hash(path);
  }

  private static int hash(RaptorPath<?> path) {
    int result = 1;

    PathLeg<?> leg = path.accessLeg();

    while (!leg.isEgressLeg()) {
      result = 31 * result + leg.toStop();
      result = 31 * result + leg.toTime();

      if (leg.isTransitLeg()) {
        result = 31 * result + leg.asTransitLeg().trip().pattern().debugInfo().hashCode();
      }
      leg = leg.nextLeg();
    }
    result = 31 * result + leg.toTime();

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o.getClass() != PathKey.class) {
      return false;
    }
    return hash == ((PathKey) o).hash;
  }

  @Override
  public int hashCode() {
    return hash;
  }
}
