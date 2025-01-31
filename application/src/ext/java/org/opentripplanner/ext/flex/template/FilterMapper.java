package org.opentripplanner.ext.flex.template;

import java.util.HashSet;
import java.util.List;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FilterMapper {
  public static FlexTripFilter mapFilters(TransitFilterRequest sr) {
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

    return new FlexTripFilter.Filter(selectedAgencies, bannedAgencies, selectedRoutes, bannedRoutes);
  }

  public static List<FlexTripFilter> mapFilters(List<TransitFilter> filters) {
    return filters.stream().map(s -> switch (s) {
      case TransitFilterRequest sr -> mapFilters(sr);
      case AllowAllTransitFilter sr -> new FlexTripFilter.AllowAll();
      case ExcludeAllTransitFilter f -> new FlexTripFilter.AllowAll();
      default -> throw new IllegalStateException("Unexpected value: " + s);
    }).distinct().toList();
  }
}
