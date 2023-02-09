package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class RouteRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean wheelchairEnabled;

  private final WheelchairPreferences wheelchairPreferences;

  private final boolean includePlannedCancellations;

  private final boolean includeRealtimeCancellations;

  private final List<TransitFilter> filters;

  private final Set<FeedScopedId> bannedTrips;

  private final boolean hasSubModeFilters;

  public RouteRequestTransitDataProviderFilter(RouteRequest request) {
    this(
      request.journey().transfer().mode() == StreetMode.BIKE,
      request.wheelchair(),
      request.preferences().wheelchair(),
      request.preferences().transit().includePlannedCancellations(),
      request.preferences().transit().includeRealtimeCancellations(),
      Set.copyOf(request.journey().transit().bannedTrips()),
      request.journey().transit().filters()
    );
  }

  // This constructor is used only for testing
  public RouteRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences,
    boolean includePlannedCancellations,
    boolean includeRealtimeCancellations,
    Set<FeedScopedId> bannedTrips,
    List<TransitFilter> filters
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairEnabled = wheelchairEnabled;
    this.wheelchairPreferences = wheelchairPreferences;
    this.includePlannedCancellations = includePlannedCancellations;
    this.includeRealtimeCancellations = includeRealtimeCancellations;
    this.bannedTrips = bannedTrips;
    this.filters = filters;
    this.hasSubModeFilters = filters.stream().anyMatch(TransitFilter::isSubModePredicate);
  }

  @Override
  public boolean hasSubModeFilters() {
    return hasSubModeFilters;
  }

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    for (TransitFilter filter : filters) {
      if (filter.matchTripPattern(tripPatternForDate.getTripPattern().getPattern())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes, boolean withFilters) {
    final Trip trip = tripTimes.getTrip();

    if (requireBikesAllowed) {
      if (bikeAccessForTrip(trip) != BikeAccess.ALLOWED) {
        return false;
      }
    }

    if (wheelchairEnabled) {
      if (
        wheelchairPreferences.trip().onlyConsiderAccessible() &&
        tripTimes.getWheelchairAccessibility() != Accessibility.POSSIBLE
      ) {
        return false;
      }
    }

    if (!includePlannedCancellations) {
      if (trip.getNetexAlteration().isCanceledOrReplaced()) {
        return false;
      }
    }

    if (!includeRealtimeCancellations) {
      if (tripTimes.isCanceled()) {
        return false;
      }
    }

    if (bannedTrips.contains(trip.getId())) {
      return false;
    }

    // Trip has to match with at least one predicate in order to be included in search. We only have
    // to this if we have mode specific filters, and not all trips on hte pattern have the same
    // mode, since that's the only thing that is trip specific
    if (withFilters) {
      for (TransitFilter f : filters) {
        if (f.matchTripTimes(tripTimes)) {
          return true;
        }
      }
      return false;
    }

    return true;
  }

  @Override
  public BitSet filterAvailableStops(RoutingTripPattern tripPattern, BitSet boardingPossible) {
    // if the user wants wheelchair-accessible routes and the configuration requires us to only
    // consider those stops which have the correct accessibility values then use only this for
    // checking whether to board/alight
    if (wheelchairEnabled && wheelchairPreferences.stop().onlyConsiderAccessible()) {
      var copy = (BitSet) boardingPossible.clone();
      // Use the and bitwise operator to add false flag to all stops that are not accessible by wheelchair
      copy.and(tripPattern.getWheelchairAccessible());

      return copy;
    }
    return boardingPossible;
  }
}
