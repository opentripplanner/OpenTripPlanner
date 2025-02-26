package org.opentripplanner.ext.flex.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Map the internal OTP filter API into the reduced, flex-specific version of it.
 */
public class FilterMapper {

  public static final String INCLUDED_AGENCIES = "includedAgencies";
  public static final String INCLUDED_ROUTES = "includedRoutes";
  public static final String EXCLUDED_AGENCIES = "excludedAgencies";
  public static final String EXCLUDED_ROUTES = "excludedRoutes";
  private final Set<FeedScopedId> bannedAgencies = new HashSet<>();
  private final Set<FeedScopedId> bannedRoutes = new HashSet<>();
  private final Set<FeedScopedId> selectedAgencies = new HashSet<>();
  private final Set<FeedScopedId> selectedRoutes = new HashSet<>();

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
      builder.withIncludedAgencies(
        FilterValues.ofEmptyIsNothing(INCLUDED_AGENCIES, selectedAgencies)
      );
    }
    if (!selectedRoutes.isEmpty()) {
      builder.withIncludedRoutes(FilterValues.ofEmptyIsNothing(INCLUDED_ROUTES, selectedRoutes));
    }
    if (!bannedAgencies.isEmpty()) {
      builder.withExcludedAgencies(
        FilterValues.ofEmptyIsEverything(EXCLUDED_AGENCIES, bannedAgencies)
      );
    }
    if (!bannedRoutes.isEmpty()) {
      builder.withExcludedRoutes(FilterValues.ofEmptyIsEverything(EXCLUDED_ROUTES, bannedRoutes));
    }
    return builder.build();
  }

  private void addFilter(TransitFilterRequest sr) {
    sr
      .not()
      .forEach(s -> {
        bannedRoutes.addAll(s.routes());
        bannedAgencies.addAll(s.agencies());
      });
    sr
      .select()
      .forEach(s -> {
        selectedRoutes.addAll(s.routes());
        selectedAgencies.addAll(s.agencies());
      });
  }
}
