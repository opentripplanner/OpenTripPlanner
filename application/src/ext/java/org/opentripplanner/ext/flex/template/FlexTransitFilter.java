package org.opentripplanner.ext.flex.template;

import java.util.List;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;

public class FlexTransitFilter {

  public static FlexTransitFilter ALLOW_ALL = new FlexTransitFilter(List.of(new FlexTripFilter.AllowAll()));
  private final List<FlexTripFilter> filters;

  public FlexTransitFilter(List<FlexTripFilter> filters) {
    this.filters = filters;
  }

  public boolean allowsTrip(ClosestTrip closestTrip){
    for(var filter : filters){
      if (!filter.allowsTrip(closestTrip.flexTrip().getTrip())){
        return false;
      }
    }
    return true;
  }

}
