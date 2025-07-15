package org.opentripplanner.apis.gtfs.support.filter;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.transit.model.site.StopType;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.opentripplanner.utils.collection.EnumSetUtils;

/**
 * A filter for {@link StopArrival} objects based on their {@link StopType}.
 */
public class StopArrivalByTypeFilter {

  @Nullable
  private final Set<StopType> allowedTypes;

  public StopArrivalByTypeFilter(@Nullable Collection<GraphQLTypes.GraphQLStopType> types) {
    CollectionUtils.requireNullOrNonEmpty(types, "Stop types must be non-empty or null");
    allowedTypes = map(types);
  }

  /**
   * Filters a list of {@link StopArrival} objects based on allowed stop types.
   * If the list of arrivals is null or no allowed stop types are configured, the
   * original list of arrivals is returned.
   */
  public List<StopArrival> filter(@Nullable List<StopArrival> arrivals) {
    if (allowedTypes == null || arrivals == null) {
      return arrivals;
    } else {
      return arrivals
        .stream()
        .filter(arrival -> allowedTypes.contains(arrival.place.stop.getStopType()))
        .toList();
    }
  }

  private static Set<StopType> map(@Nullable Collection<GraphQLTypes.GraphQLStopType> types) {
    if (types == null) {
      return null;
    } else {
      var allowed = types
        .stream()
        .map(type ->
          switch (type) {
            case LOCATION -> StopType.FLEXIBLE_AREA;
            case LOCATION_GROUP -> StopType.FLEXIBLE_GROUP;
            case STOP -> StopType.REGULAR;
          }
        )
        .collect(Collectors.toSet());
      return EnumSetUtils.unmodifiableEnumSet(allowed, StopType.class);
    }
  }
}
