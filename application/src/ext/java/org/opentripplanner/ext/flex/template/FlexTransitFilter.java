package org.opentripplanner.ext.flex.template;

import java.util.List;

public class FlexTransitFilter {

  public static FlexTransitFilter ALLOW_ALL = new FlexTransitFilter(List.of(new FlexTripFilterRequest.AllowAll()));
  private final List<FlexTripFilterRequest> filters;

  public FlexTransitFilter(List<FlexTripFilterRequest> filters) {
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
