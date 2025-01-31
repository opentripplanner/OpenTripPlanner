package org.opentripplanner.ext.flex.template;

import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

sealed interface FlexTripFilter {

  boolean allowsTrip(Trip trip);

  record Filter(Set<FeedScopedId> selectedAgencies, Set<FeedScopedId> excludedAgencies,
                Set<FeedScopedId> selectedRoutes,
                Set<FeedScopedId> excludedRoutes) implements FlexTripFilter {
    @Override
    public boolean allowsTrip(Trip trip) {
      if (containsSelect()) {
        return selectedRoutes.contains(trip.getRoute().getId()) || selectedAgencies.contains(trip.getRoute().getAgency().getId());
      } else if (containsBan()) {
        return !excludedRoutes.contains(trip.getRoute().getId()) && !excludedAgencies.contains(trip.getRoute().getAgency().getId());
      } else {
        return true;
      }
    }

    private boolean containsBan() {
      return !excludedAgencies.isEmpty() || !excludedRoutes.isEmpty();
    }

    boolean containsSelect() {
      return !selectedAgencies.isEmpty() || !selectedRoutes.isEmpty();
    }
  }

  final class AllowAll implements FlexTripFilter {
    @Override
    public boolean allowsTrip(Trip trip) {
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof AllowAll;
    }
  }
}
