package org.opentripplanner.ext.sorlandsbanen;

import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;

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
