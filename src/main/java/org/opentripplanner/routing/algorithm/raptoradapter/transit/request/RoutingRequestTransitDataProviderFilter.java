package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final WheelchairAccessibilityRequest wheelchairAccessibility;

  private final boolean includePlannedCancellations;

  private final AllowTransitModeFilter transitModeFilter;

  private final Set<FeedScopedId> bannedRoutes;

  private final Set<FeedScopedId> bannedTrips;

  public RoutingRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    WheelchairAccessibilityRequest accessibility,
    boolean includePlannedCancellations,
    Collection<MainAndSubMode> allowedTransitModes,
    Set<FeedScopedId> bannedRoutes,
    Set<FeedScopedId> bannedTrips
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairAccessibility = accessibility;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = bannedRoutes;
    this.bannedTrips = bannedTrips;
    this.transitModeFilter = AllowTransitModeFilter.of(allowedTransitModes);
  }

  public RoutingRequestTransitDataProviderFilter(
    RoutingRequest request,
    TransitService transitService
  ) {
    this(
      request.modes.transferMode == StreetMode.BIKE,
      request.wheelchairAccessibility,
      request.includePlannedCancellations,
      request.modes.transitModes,
      request.getBannedRoutes(transitService.getAllRoutes()),
      request.bannedTrips
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

    if (wheelchairAccessibility.enabled()) {
      if (
        wheelchairAccessibility.trip().onlyConsiderAccessible() &&
        tripTimes.getWheelchairAccessibility() != WheelchairAccessibility.POSSIBLE
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
    if (
      wheelchairAccessibility.enabled() && wheelchairAccessibility.stop().onlyConsiderAccessible()
    ) {
      var copy = (BitSet) boardingPossible.clone();
      // Use the and bitwise operator to add false flag to all stops that are not accessible by wheelchair
      copy.and(tripPattern.getWheelchairAccessible());

      return copy;
    }
    return boardingPossible;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().route().getId();
    return !bannedRoutes.contains(routeId);
  }
}
