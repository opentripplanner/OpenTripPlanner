package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class ExcludeAllTransitFilter implements Cloneable, Serializable, TransitFilter {

  private static final ExcludeAllTransitFilter INSTANCE = new ExcludeAllTransitFilter();

  private ExcludeAllTransitFilter() {}

  public static ExcludeAllTransitFilter of() {
    return INSTANCE;
  }

  @Override
  public boolean matchRoute(Route route) {
    return false;
  }

  @Override
  public boolean matchTripTimes(TripTimes trip) {
    return false;
  }

  @Override
  public TransitFilter clone() {
    try {
      return (ExcludeAllTransitFilter) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
