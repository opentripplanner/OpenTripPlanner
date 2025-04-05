package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * This filter will include everything.
 */
public class AllowAllTransitFilter implements Serializable, TransitFilter {

  private static final AllowAllTransitFilter INSTANCE = new AllowAllTransitFilter();

  private AllowAllTransitFilter() {}

  public static AllowAllTransitFilter of() {
    return INSTANCE;
  }

  @Override
  public boolean matchTripPattern(TripPattern tripPattern) {
    return true;
  }

  @Override
  public boolean matchTripTimes(TripTimes trip) {
    return true;
  }

  @Override
  public String toString() {
    return "ALLOW_ALL";
  }
}
