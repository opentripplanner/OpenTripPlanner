package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
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
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

public class RouteRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean wheelchairEnabled;

  private final WheelchairPreferences wheelchairPreferences;

  private final boolean includePlannedCancellations;

  private final List<TransitFilter> filters;

  private final List<FeedScopedId> bannedTrips;

  private final Set<FeedScopedId> bannedRoutes;

  private final boolean hasSubModeFilters;

  public RouteRequestTransitDataProviderFilter(
    RouteRequest request,
    TransitService transitService
  ) {
    this(
      request.journey().transfer().mode() == StreetMode.BIKE,
      request.wheelchair(),
      request.preferences().wheelchair(),
      request.preferences().transit().includePlannedCancellations(),
      request.journey().transit().bannedTrips(),
      request.journey().transit().filters(),
      transitService
    );
  }

  public RouteRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences,
    boolean includePlannedCancellations,
    List<FeedScopedId> bannedTrips,
    List<TransitFilter> filters,
    TransitService transitService
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairEnabled = wheelchairEnabled;
    this.wheelchairPreferences = wheelchairPreferences;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = bannedRoutes(filters, transitService.getAllRoutes());
    this.bannedTrips = bannedTrips;
    this.filters = filters;
    this.hasSubModeFilters = filters.stream().anyMatch(TransitFilter::isSubModePredicate);
  }

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    return bannedRoutes.isEmpty() || routeIsNotBanned(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
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
      //noinspection RedundantIfStatement
      if (trip.getNetexAlteration().isCanceledOrReplaced()) {
        return false;
      }
    }

    if (bannedTrips.contains(trip.getId())) {
      return false;
    }

    // TODO: 2022-12-13 filters: this is expensive
    //  we only have to do it if we have submodes in the filters
    //  in the future we will make sure that we have separate routing trip pattern for each submode
    //  then we do not have to do that
    // trip has to match with at least one predicate in order to be included in search
    if (hasSubModeFilters) {
      // we only have to this if we have submode specific filter
      //  since that's the only thing that is trip specific
      return filters.stream().anyMatch(f -> f.matchTripTimes(tripTimes));
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

  public static Set<FeedScopedId> bannedRoutes(
    List<TransitFilter> filters,
    Collection<Route> routes
  ) {
    Set<FeedScopedId> ret = new HashSet<>();
    for (Route route : routes) {
      // Route have to match with at least predicate in order to be included in search
      if (filters.stream().noneMatch(f -> f.matchRoute(route))) {
        ret.add(route.getId());
      }
    }
    return ret;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().route().getId();
    return !bannedRoutes.contains(routeId);
  }
}
