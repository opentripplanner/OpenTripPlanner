package org.opentripplanner.ext.flex.filter;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Map the internal OTP filter API into the reduced, flex-specific version of it.
 */
public class FilterMapper {

  private final List<FeedScopedId> excludedAgencies = new ArrayList<>();
  private final List<FeedScopedId> excludedRoutes = new ArrayList<>();
  private final List<FeedScopedId> selectedAgencies = new ArrayList<>();
  private final List<FeedScopedId> selectedRoutes = new ArrayList<>();

  private FilterMapper() {}

  public static TripRequest map(List<TransitFilter> filters) {
    var mapper = new FilterMapper();
    return mapper.mapFilters(filters);
  }

  private TripRequest mapFilters(List<TransitFilter> filters) {
    var builder = TripRequest.of();

    for (TransitFilter filter : filters) {
      if (filter instanceof TransitFilterRequest sr) {
        addFilter(sr);
      } else if (
        !(filter instanceof AllowAllTransitFilter) && !(filter instanceof ExcludeAllTransitFilter)
      ) {
        throw new IllegalStateException("Unexpected value: " + filter);
      }
    }
    if (!selectedAgencies.isEmpty()) {
      builder.withIncludeAgencies(selectedAgencies);
    }
    if (!selectedRoutes.isEmpty()) {
      builder.withIncludeRoutes(selectedRoutes);
    }
    if (!excludedAgencies.isEmpty()) {
      builder.withExcludeAgencies(excludedAgencies);
    }
    if (!excludedRoutes.isEmpty()) {
      builder.withExcludeRoutes(excludedRoutes);
    }
    return builder.build();
  }

  private void addFilter(TransitFilterRequest sr) {
    sr
      .not()
      .forEach(s -> {
        excludedRoutes.addAll(s.routes());
        excludedAgencies.addAll(s.agencies());
      });
    sr
      .select()
      .forEach(s -> {
        selectedRoutes.addAll(s.routes());
        selectedAgencies.addAll(s.agencies());
      });
  }
}
