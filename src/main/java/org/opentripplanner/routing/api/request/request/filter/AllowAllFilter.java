package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class AllowAllFilter implements Cloneable, Serializable, FilterPredicate {

  @Override
  public boolean routePredicate(Route route) {
    return true;
  }

  @Override
  public boolean tripTimesPredicate(TripTimes trip) {
    return true;
  }

  @Override
  public AllowAllFilter clone() {
    try {
      return (AllowAllFilter) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
