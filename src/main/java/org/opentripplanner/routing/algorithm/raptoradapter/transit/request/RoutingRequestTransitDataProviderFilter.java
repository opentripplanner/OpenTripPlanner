package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Set;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean requireWheelchairAccessible;

  private final boolean includePlannedCancellations;

  private final Predicate<Trip> transitModeIsAllowed;

  private final Set<FeedScopedId> bannedRoutes;

  public RoutingRequestTransitDataProviderFilter(
      boolean requireBikesAllowed,
      boolean requireWheelchairAccessible,
      boolean includePlannedCancellations,
      Set<AllowedTransitMode> allowedTransitModes,
      Set<FeedScopedId> bannedRoutes
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.requireWheelchairAccessible = requireWheelchairAccessible;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = bannedRoutes;
    boolean hasOnlyMainModeFilters = allowedTransitModes.stream()
            .noneMatch(AllowedTransitMode::hasSubMode);

    // It is much faster to do a lookup in an EnumSet, so we use it if we don't want to filter
    // using submodes
    if (hasOnlyMainModeFilters) {
      EnumSet<TransitMode> allowedMainModes = allowedTransitModes.stream()
              .map(AllowedTransitMode::getMainMode)
              .collect(Collectors.toCollection(() -> EnumSet.noneOf(TransitMode.class)));
      transitModeIsAllowed = (Trip trip) -> allowedMainModes.contains(trip.getMode());
    } else {
      transitModeIsAllowed = (Trip trip) -> {
        TransitMode transitMode = trip.getMode();
        String netexSubmode = trip.getNetexSubmode();
        return allowedTransitModes.stream().anyMatch(m -> m.allows(transitMode, netexSubmode));
      };
    }
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
    return routeIsNotBanned(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    if (!transitModeIsAllowed.test(tripTimes.getTrip())) {
      return false;
    }

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

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }
}
