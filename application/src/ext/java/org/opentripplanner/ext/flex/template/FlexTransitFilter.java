package org.opentripplanner.ext.flex.template;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.transit.service.TransitService;

public class FlexTransitFilter {

  private final TransitService transitService;
  private final List<TransitFilter> filters;

  public FlexTransitFilter(TransitService transitService, List<TransitFilter> filters) {
    this.transitService = transitService;
    this.filters = filters;
  }

  public boolean matchesTransitFilters(ClosestTrip trip) {
    if(disablesTransit()) return true;
    var t = trip.flexTrip().getTrip() ;
    var pattern = Objects.requireNonNull(transitService.findPattern(t), "flex trip doesn't have a pattern.");
    for (TransitFilter filter : filters) {
      if (filter.matchTripPattern(pattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean disablesTransit(){
    return filters.size() == 1 && filters.getFirst() == ExcludeAllTransitFilter.of();
  }
}
