package org.opentripplanner.graph_builder.module.transfer.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Combines multiple {@link NearbyStopFilter}s into a single filter.
 * <p>
 * This filter applies all configured filters and returns the union of their results. A stop is
 * included if ANY of the component filters include it (OR logic for from-stops, union for
 * to-stops).
 */
class CompositeNearbyStopFilter implements NearbyStopFilter {

  private final List<NearbyStopFilter> filters;

  private CompositeNearbyStopFilter(List<NearbyStopFilter> filters) {
    this.filters = filters;
  }

  static Builder of() {
    return new Builder();
  }

  @Override
  public boolean includeFromStop(FeedScopedId id, boolean reverseDirection) {
    for (NearbyStopFilter filter : filters) {
      if (filter.includeFromStop(id, reverseDirection)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Collection<NearbyStop> filterToStops(
    Collection<NearbyStop> nearbyStops,
    boolean reverseDirection
  ) {
    Set<NearbyStop> result = new HashSet<>();

    for (NearbyStopFilter it : filters) {
      result.addAll(it.filterToStops(nearbyStops, reverseDirection));
    }
    return result;
  }

  static class Builder {

    List<NearbyStopFilter> filters = new ArrayList<>();

    Builder add(NearbyStopFilter filter) {
      filters.add(filter);
      return this;
    }

    NearbyStopFilter build() {
      if (filters.size() == 1) {
        return filters.getFirst();
      }
      return new CompositeNearbyStopFilter(filters);
    }
  }
}
