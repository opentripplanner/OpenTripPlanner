package org.opentripplanner.ext.flex.template;

import java.util.HashSet;
import java.util.List;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FilterMapper {
  public static FlexTripFilterRequest mapFilters(TransitFilterRequest sr) {
    var bannedAgencies = new HashSet<FeedScopedId>();
    var bannedRoutes = new HashSet<FeedScopedId>();
    var selectedAgencies = new HashSet<FeedScopedId>();
    var selectedRoutes = new HashSet<FeedScopedId>();

    sr.not().forEach(s -> {
      bannedRoutes.addAll(s.routes());
      bannedAgencies.addAll(s.agencies());
    });
    sr.select().forEach(s -> {
      selectedRoutes.addAll(s.routes());
      selectedAgencies.addAll(s.agencies());
    });

    return new FlexTripFilterRequest.Filter(selectedAgencies, bannedAgencies, selectedRoutes, bannedRoutes);
  }

  public static List<FlexTripFilterRequest> mapFilters(List<TransitFilter> filters) {
    return filters.stream().map(s -> switch (s) {
      case TransitFilterRequest sr -> mapFilters(sr);
      case AllowAllTransitFilter sr -> new FlexTripFilterRequest.AllowAll();
      // excluding all transit means all fixed schedule transit but flex can still be use for
      // direct routes, therefor it means to everything in the contrext of flex
      case ExcludeAllTransitFilter f -> new FlexTripFilterRequest.AllowAll();
      default -> throw new IllegalStateException("Unexpected value: " + s);
    }).distinct().toList();
  }
}
