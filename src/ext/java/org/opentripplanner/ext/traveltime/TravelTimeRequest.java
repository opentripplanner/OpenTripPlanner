package org.opentripplanner.ext.traveltime;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * A request for an travel time map.
 *
 * @author laurent
 */
public class TravelTimeRequest {

  public final List<Duration> cutoffs;

  public boolean includeDebugGeometry;

  public int precisionMeters = 200;

  public int offRoadDistanceMeters = 150;

  public final Duration maxCutoff;

  public Duration maxAccessDuration = Duration.ofMinutes(45);

  public TravelTimeRequest(List<Duration> cutoffList) {
    this.cutoffs = cutoffList;
    this.maxCutoff = cutoffs.stream().max(Duration::compareTo).orElseThrow();
    if (maxCutoff.compareTo(maxAccessDuration) < 0) {
      maxAccessDuration = maxCutoff;
    }
  }

  @Override
  public int hashCode() {
    return cutoffs.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof TravelTimeRequest otherReq) {
      return this.cutoffs.equals(otherReq.cutoffs);
    }
    return false;
  }

  public String toString() {
    return String.format(
      "<isochrone request, cutoff=%s sec, precision=%d meters>",
      Arrays.toString(cutoffs.toArray()),
      precisionMeters
    );
  }
}
