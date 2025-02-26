package org.opentripplanner.ext.flex.filter;

import java.util.HashSet;
import java.util.List;
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

  public static TripRequest mapFilters(List<TransitFilter> filters) {
    if (filters.size() > 1) {
      throw new IllegalArgumentException("Only one transit filter is allowed");
    }
    return filters
      .stream()
      .map(s ->
        switch (s) {
          case TransitFilterRequest sr -> mapFilters(sr);
          case AllowAllTransitFilter sr -> TripRequest.of().build();
          // excluding all transit means all fixed schedule transit but flex can still be use for
          // direct routes, therefore it means to allow all trips in the context of flex
          case ExcludeAllTransitFilter f -> TripRequest.of().build();
          default -> throw new IllegalStateException("Unexpected value: " + s);
        }
      )
      .distinct()
      .toList()
      .getFirst();
  }

  private static TripRequest mapFilters(TransitFilterRequest sr) {
    var bannedAgencies = new HashSet<FeedScopedId>();
    var bannedRoutes = new HashSet<FeedScopedId>();
    var selectedAgencies = new HashSet<FeedScopedId>();
    var selectedRoutes = new HashSet<FeedScopedId>();

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

    return TripRequest
      .of()
      .withIncludedAgencies(FilterValues.ofEmptyIsNothing("includedAgencies", selectedAgencies))
      .withIncludedRoutes(FilterValues.ofEmptyIsNothing("includedRoutes", selectedRoutes))
      .withExcludedAgencies(FilterValues.ofEmptyIsEverything("excludedAgencies", bannedAgencies))
      .withExcludedRoutes(FilterValues.ofEmptyIsEverything("excludedRoutes", bannedRoutes))
      .build();
  }
}
