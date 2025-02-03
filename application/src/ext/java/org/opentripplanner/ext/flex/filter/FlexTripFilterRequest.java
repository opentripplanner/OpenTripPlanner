package org.opentripplanner.ext.flex.filter;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public sealed interface FlexTripFilterRequest {
  boolean allowsTrip(Trip trip);

  /**
   * A filter that allows you to select (whitelist) or exclude (blacklist)
   */
  final class Filter implements FlexTripFilterRequest {

    private final Set<FeedScopedId> selectedAgencies;
    private final Set<FeedScopedId> excludedAgencies;
    private final Set<FeedScopedId> selectedRoutes;
    private final Set<FeedScopedId> excludedRoutes;

    /**
     *
     */
    public Filter(
      Set<FeedScopedId> selectedAgencies,
      Set<FeedScopedId> excludedAgencies,
      Set<FeedScopedId> selectedRoutes,
      Set<FeedScopedId> excludedRoutes
    ) {
      this.selectedAgencies = selectedAgencies;
      this.excludedAgencies = excludedAgencies;
      this.selectedRoutes = selectedRoutes;
      this.excludedRoutes = excludedRoutes;
    }

    @Override
    public boolean allowsTrip(Trip trip) {
      var agencyId = trip.getRoute().getAgency().getId();
      var routeId = trip.getRoute().getId();
      if (containsSelect()) {
        return selectedRoutes.contains(routeId) || selectedAgencies.contains(agencyId);
      } else if (containsBan()) {
        if (excludedRoutes.contains(routeId)) {
          return false;
        } else return !excludedAgencies.contains(agencyId);
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

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (Filter) obj;
      return (
        Objects.equals(this.selectedAgencies, that.selectedAgencies) &&
        Objects.equals(this.excludedAgencies, that.excludedAgencies) &&
        Objects.equals(this.selectedRoutes, that.selectedRoutes) &&
        Objects.equals(this.excludedRoutes, that.excludedRoutes)
      );
    }

    @Override
    public int hashCode() {
      return Objects.hash(selectedAgencies, excludedAgencies, selectedRoutes, excludedRoutes);
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(Filter.class)
        .addCol("selectedAgencies", selectedAgencies)
        .addCol("excludedAgencies", excludedAgencies)
        .addCol("selectedRoutes", selectedRoutes)
        .addCol("excludedRoutes", excludedRoutes)
        .toString();
    }
  }

  /**
   * The default filter which allows all flex trips to be used for routing.
   */
  final class AllowAll implements FlexTripFilterRequest {

    private static final AllowAll INSTANCE = new AllowAll();

    private AllowAll() {}

    public static AllowAll of() {
      return INSTANCE;
    }

    @Override
    public boolean allowsTrip(Trip trip) {
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof AllowAll;
    }

    @Override
    public String toString() {
      return AllowAll.class.getSimpleName();
    }
  }
}
