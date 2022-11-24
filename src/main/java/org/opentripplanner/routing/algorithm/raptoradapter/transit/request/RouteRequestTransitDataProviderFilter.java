package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.TransitRequest;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
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

  private final Map<String, AllowTransitModeFilter> feedIdSpecificTransitModeFilter;

  public RouteRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences,
    boolean includePlannedCancellations,
    Collection<FeedScopedId> bannedRoutes,
    TransitRequest transitRequest
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairEnabled = wheelchairEnabled;
    this.wheelchairPreferences = wheelchairPreferences;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = Set.copyOf(bannedRoutes);
    this.bannedTrips = Set.copyOf(transitRequest.commonFilters().bannedTrips());
    this.transitModeFilter = AllowTransitModeFilter.of(transitRequest.commonFilters().modes());

    var feedIdSpecificTransitModeFilter = new HashMap<String, AllowTransitModeFilter>();
    for (var entry : transitRequest.feedIdSpecificFilters().entrySet()) {
      feedIdSpecificTransitModeFilter.put(
        entry.getKey(),
        AllowTransitModeFilter.of(entry.getValue().modes())
      );
    }
    this.feedIdSpecificTransitModeFilter = feedIdSpecificTransitModeFilter;
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
      bannedRoutes(request.journey().transit(), transitService.getAllRoutes()),
      request.journey().transit()
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

    // Check if there is feedId-specific filter, otherwise use common one
    var transitModeFilter = feedIdSpecificTransitModeFilter.getOrDefault(
      trip.getId().getFeedId(),
      this.transitModeFilter
    );

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
    TransitRequest transitRequest,
    Collection<Route> routes
  ) {
    if (
      transitRequest.feedIdSpecificFilters().isEmpty() &&
      transitRequest.commonFilters().bannedAgencies().isEmpty() &&
      transitRequest.commonFilters().bannedRoutes().isEmpty() &&
      transitRequest.commonFilters().whiteListedAgencies().isEmpty() &&
      transitRequest.commonFilters().whiteListedRoutes().isEmpty() &&
      transitRequest.commonFilters().whiteListedGroupsOfRoutes().isEmpty() &&
      transitRequest.commonFilters().bannedGroupsOfRoutes().isEmpty()
    ) {
      // nothing to filter with
      return List.of();
    }

    List<FeedScopedId> ret = new ArrayList<>();
    for (Route route : routes) {
      var filter = transitRequest.feedIdSpecificFilter(route.getId().getFeedId());
      if (filter == null) filter = transitRequest.commonFilters();

      // TODO: 2022-11-24 do we really need to create a copy here?
      Set<FeedScopedId> bannedAgencies = Set.copyOf(filter.bannedAgencies());
      Set<FeedScopedId> whiteListedAgencies = Set.copyOf(filter.whiteListedAgencies());
      Set<FeedScopedId> whiteListedGroupsOfRoutes = Set.copyOf(filter.whiteListedGroupsOfRoutes());
      Set<FeedScopedId> bannedGroupOfRoutes = Set.copyOf(filter.bannedGroupsOfRoutes());

      if (
        routeIsBanned(
          bannedAgencies,
          filter.bannedRoutes(),
          whiteListedAgencies,
          filter.whiteListedRoutes(),
          whiteListedGroupsOfRoutes,
          bannedGroupOfRoutes,
          route
        )
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
    Set<FeedScopedId> whiteListedGroupsOfRoutes,
    Set<FeedScopedId> bannedGroupsOfRoutes,
    Route route
  ) {
    if (
      bannedAgencies.isEmpty() &&
      bannedRoutes.isEmpty() &&
      whiteListedAgencies.isEmpty() &&
      whiteListedRoutes.isEmpty() &&
      whiteListedGroupsOfRoutes.isEmpty() &&
      bannedGroupsOfRoutes.isEmpty()
    ) {
      return false;
    }

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

    var groupOfRoutesIDs = route
      .getGroupsOfRoutes()
      .stream()
      .map(AbstractTransitEntity::getId)
      .toList();

    if (!Collections.disjoint(bannedGroupsOfRoutes, groupOfRoutesIDs)) {
      return true;
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

    if (!whiteListedGroupsOfRoutes.isEmpty()) {
      whiteListInUse = true;
      for (var id : groupOfRoutesIDs) {
        if (whiteListedGroupsOfRoutes.contains(id)) {
          whiteListed = true;
          break;
        }
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
