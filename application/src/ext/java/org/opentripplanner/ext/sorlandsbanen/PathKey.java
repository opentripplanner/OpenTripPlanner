package org.opentripplanner.ext.sorlandsbanen;

import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;


/**
 * Uses a hash to create a key for access, egress and transit legs in a path. Transfers
 * are not included. The key is used to exclude duplicates. This approach may drop valid results
 * when there is a hash collision, but this whole sandbox feature is a hack - so we can tolerate
 * this here.
 */
final class PathKey {

  private final int hash;

  PathKey(RaptorPath<?> path) {
    this.hash = hash(path);
  }

  private static int hash(RaptorPath<?> path) {
    if (path == null) {
      return 0;
    }
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
