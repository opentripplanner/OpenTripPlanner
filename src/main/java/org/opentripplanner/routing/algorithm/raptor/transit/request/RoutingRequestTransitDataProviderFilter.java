package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.EnumSet;
import java.util.Set;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean requireWheelchairAccessible;

  private final boolean includePlannedCancellations;

  private final Set<TransitMode> transitModes;

  private final Set<FeedScopedId> bannedRoutes;

  public RoutingRequestTransitDataProviderFilter(
      boolean requireBikesAllowed,
      boolean requireWheelchairAccessible,
      boolean includePlannedCancellations,
      Set<TransitMode> transitModes,
      Set<FeedScopedId> bannedRoutes
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.requireWheelchairAccessible = requireWheelchairAccessible;
    this.includePlannedCancellations = includePlannedCancellations;
    this.transitModes = transitModes.isEmpty()
        ? EnumSet.noneOf(TransitMode.class)
        : EnumSet.copyOf(transitModes);
    this.bannedRoutes = bannedRoutes;
  }

  public RoutingRequestTransitDataProviderFilter(
          RoutingRequest request,
          GraphIndex graphIndex
  ) {
    this(
        request.modes.transferMode == StreetMode.BIKE,
        request.wheelchairAccessible,
        request.includePlannedCancellations,
        request.modes.transitModes,
        request.getBannedRoutes(graphIndex.getAllRoutes())
    );
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    return routeIsNotBanned(tripPatternForDate) && transitModeIsAllowed(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    if (requireBikesAllowed) {
      return bikeAccessForTrip(tripTimes.getTrip()) == BikeAccess.ALLOWED;
    }

    if (requireWheelchairAccessible) {
      return tripTimes.getTrip().getWheelchairAccessible() == 1;
    }

    if (!includePlannedCancellations) {
      return !tripTimes.getTrip().getTripAlteration().isCanceledOrReplaced();
    }

    return true;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().getPattern().getRoute().getId();
    return !bannedRoutes.contains(routeId);
  }

  private boolean transitModeIsAllowed(TripPatternForDate tripPatternForDate) {
    TransitMode transitMode = tripPatternForDate.getTripPattern().getTransitMode();
    return transitModes.contains(transitMode);
  }

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }
}
