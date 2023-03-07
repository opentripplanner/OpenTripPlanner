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

  public final boolean includeDebugGeometry = false;

  public final int precisionMeters = 200;

  public final int offRoadDistanceMeters = 150;

  public final Duration maxCutoff;

  public final Duration maxAccessDuration;
  public final Duration maxEgressDuration;

  public TravelTimeRequest(
    List<Duration> cutoffList,
    Duration defaultAccessDuration,
    Duration defaultEgressDuration
  ) {
    this.cutoffs = cutoffList;
    this.maxCutoff = cutoffs.stream().max(Duration::compareTo).orElseThrow();
    if (maxCutoff.compareTo(defaultAccessDuration) < 0) {
      maxAccessDuration = maxCutoff;
    } else {
      maxAccessDuration = defaultAccessDuration;
    }

    if (maxCutoff.compareTo(defaultEgressDuration) < 0) {
      maxEgressDuration = maxCutoff;
    } else {
      maxEgressDuration = defaultEgressDuration;
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
