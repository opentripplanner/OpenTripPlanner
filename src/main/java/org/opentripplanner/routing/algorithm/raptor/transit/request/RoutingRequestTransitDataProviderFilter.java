package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.EnumSet;
import java.util.Set;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean requireWheelchairAccessible;

  private final Set<TransitMode> transitModes;

  private final Set<FeedScopedId> bannedRoutes;

  public RoutingRequestTransitDataProviderFilter(
      boolean requireBikesAllowed,
      boolean requireWheelchairAccessible,
      Set<TransitMode> transitModes,
      Set<FeedScopedId> bannedRoutes
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.requireWheelchairAccessible = requireWheelchairAccessible;
    this.transitModes = transitModes.isEmpty()
        ? EnumSet.noneOf(TransitMode.class)
        : EnumSet.copyOf(transitModes);
    this.bannedRoutes = bannedRoutes;
  }

  public RoutingRequestTransitDataProviderFilter(RoutingRequest request) {
    this(
        request.modes.directMode == StreetMode.BIKE,
        request.wheelchairAccessible,
        request.modes.transitModes,
        request.rctx.bannedRoutes
    );
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    return routeIsNotBanned(tripPatternForDate) && transitModeIsAllowed(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    if (requireBikesAllowed) {
      return BikeAccess.fromTrip(tripTimes.trip) == BikeAccess.ALLOWED;
    }

    if (requireWheelchairAccessible) {
      return tripTimes.trip.getWheelchairAccessible() == 1;
    }

    return true;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().getPattern().route.getId();
    return !bannedRoutes.contains(routeId);
  }

  private boolean transitModeIsAllowed(TripPatternForDate tripPatternForDate) {
    TransitMode transitMode = tripPatternForDate.getTripPattern().getTransitMode();
    return transitModes.contains(transitMode);
  }
}
