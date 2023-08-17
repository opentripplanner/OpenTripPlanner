package org.opentripplanner.smoketest.util;

import java.util.Set;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.parameters.TripPlanParameters;

public record SmokeTestRequest(
  Coordinate from,
  Coordinate to,
  Set<RequestMode> modes,
  boolean arriveBy
) {
  public SmokeTestRequest(Coordinate from, Coordinate to, Set<RequestMode> modes) {
    this(from, to, modes, false);
  }

  public TripPlanParameters.SearchDirection searchDirection() {
    if (arriveBy) {
      return TripPlanParameters.SearchDirection.ARRIVE_BY;
    } else {
      return TripPlanParameters.SearchDirection.DEPART_AT;
    }
  }
}
