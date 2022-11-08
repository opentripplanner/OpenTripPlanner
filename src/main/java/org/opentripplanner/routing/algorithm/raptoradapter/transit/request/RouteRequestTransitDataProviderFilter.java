package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
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

  private final AllowTransitModeFilter transitModeFilter;

  private final Set<FeedScopedId> bannedRoutes;

  private final Set<FeedScopedId> bannedTrips;

  public RouteRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences,
    boolean includePlannedCancellations,
    Collection<MainAndSubMode> allowedTransitModes,
    Collection<FeedScopedId> bannedRoutes,
    Collection<FeedScopedId> bannedTrips
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairEnabled = wheelchairEnabled;
    this.wheelchairPreferences = wheelchairPreferences;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = Set.copyOf(bannedRoutes);
    this.bannedTrips = Set.copyOf(bannedTrips);
    this.transitModeFilter = AllowTransitModeFilter.of(allowedTransitModes);
  }

  public RouteRequestTransitDataProviderFilter(
    RouteRequest request,
    TransitService transitService
  ) {
    this(
      request.journey().transfer().mode() == StreetMode.BIKE,
      request.wheelchair(),
      request.preferences().wheelchair(),
      request.preferences().transit().includePlannedCancellations(),
      request.journey().transit().modes(),
      bannedRoutes(
        request.journey().transit().bannedAgencies(),
        request.journey().transit().bannedRoutes(),
        request.journey().transit().whiteListedAgencies(),
        request.journey().transit().whiteListedRoutes(),
        transitService.getAllRoutes()
      ),
      request.journey().transit().bannedTrips()
    );
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
    if (!transitModeFilter.allows(trip.getMode(), trip.getNetexSubMode())) {
      return false;
    }

    if (!bannedTrips.isEmpty() && bannedTrips.contains(trip.getId())) {
      return false;
    }

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

  public static List<FeedScopedId> bannedRoutes(
    Collection<FeedScopedId> bannedAgenciesCollection,
    RouteMatcher bannedRoutes,
    Collection<FeedScopedId> whiteListedAgenciesCollection,
    RouteMatcher whiteListedRoutes,
    Collection<Route> routes
  ) {
    if (
      bannedRoutes.isEmpty() &&
      bannedAgenciesCollection.isEmpty() &&
      whiteListedRoutes.isEmpty() &&
      whiteListedAgenciesCollection.isEmpty()
    ) {
      return List.of();
    }

    Set<FeedScopedId> bannedAgencies = Set.copyOf(bannedAgenciesCollection);
    Set<FeedScopedId> whiteListedAgencies = Set.copyOf(whiteListedAgenciesCollection);

    List<FeedScopedId> ret = new ArrayList<>();
    for (Route route : routes) {
      if (
        routeIsBanned(bannedAgencies, bannedRoutes, whiteListedAgencies, whiteListedRoutes, route)
      ) {
        ret.add(route.getId());
      }
    }
    return ret;
  }

  /**
   * Checks if the route is banned. Also, if whitelisting is used, the route (or its agency) has to
   * be whitelisted in order to not count as banned.
   *
   * @return True if the route is banned
   */
  private static boolean routeIsBanned(
    Set<FeedScopedId> bannedAgencies,
    RouteMatcher bannedRoutes,
    Set<FeedScopedId> whiteListedAgencies,
    RouteMatcher whiteListedRoutes,
    Route route
  ) {
    /* check if agency is banned for this plan */
    if (!bannedAgencies.isEmpty()) {
      if (bannedAgencies.contains(route.getAgency().getId())) {
        return true;
      }
    }

    /* check if route banned for this plan */
    if (!bannedRoutes.isEmpty()) {
      if (bannedRoutes.matches(route)) {
        return true;
      }
    }

    boolean whiteListed = false;
    boolean whiteListInUse = false;

    /* check if agency is whitelisted for this plan */
    if (!whiteListedAgencies.isEmpty()) {
      whiteListInUse = true;
      if (whiteListedAgencies.contains(route.getAgency().getId())) {
        whiteListed = true;
      }
    }

    /* check if route is whitelisted for this plan */
    if (!whiteListedRoutes.isEmpty()) {
      whiteListInUse = true;
      if (whiteListedRoutes.matches(route)) {
        whiteListed = true;
      }
    }

    if (whiteListInUse && !whiteListed) {
      return true;
    }

    return false;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().route().getId();
    return !bannedRoutes.contains(routeId);
  }
}
