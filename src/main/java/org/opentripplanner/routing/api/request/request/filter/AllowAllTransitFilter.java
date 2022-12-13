package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class AllowAllTransitFilter implements Cloneable, Serializable, TransitFilter {

  private static final AllowAllTransitFilter INSTANCE = new AllowAllTransitFilter();

  private AllowAllTransitFilter() {}

  public static AllowAllTransitFilter of() {
    return INSTANCE;
  }

  @Override
  public boolean matchRoute(Route route) {
    return true;
  }

  @Override
  public boolean matchTripTimes(TripTimes trip) {
    return true;
  }

  @Override
  public AllowAllTransitFilter clone() {
    try {
      return (AllowAllTransitFilter) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
