package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class RouteRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean requireCarsAllowed;

  private final boolean wheelchairEnabled;

  private final WheelchairPreferences wheelchairPreferences;

  private final boolean includePlannedCancellations;

  private final boolean includeRealtimeCancellations;

  /**
   * This is stored as an array, as they are iterated over for each trip when filtering transit
   * data. Iterator creation is relatively expensive compared to iterating over a short array.
   */
  private final TransitFilter[] filters;

  private final Set<FeedScopedId> bannedTrips;

  private final boolean hasSubModeFilters;

  public RouteRequestTransitDataProviderFilter(RouteRequest request) {
    this(
      request.journey().transfer().mode() == StreetMode.BIKE,
      request.journey().transfer().mode() == StreetMode.CAR,
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
    boolean requireCarsAllowed,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences,
    boolean includePlannedCancellations,
    boolean includeRealtimeCancellations,
    Set<FeedScopedId> bannedTrips,
    List<TransitFilter> filters
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.requireCarsAllowed = requireCarsAllowed;
    this.wheelchairEnabled = wheelchairEnabled;
    this.wheelchairPreferences = wheelchairPreferences;
    this.includePlannedCancellations = includePlannedCancellations;
    this.includeRealtimeCancellations = includeRealtimeCancellations;
    this.bannedTrips = bannedTrips;
    this.filters = filters.toArray(TransitFilter[]::new);
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

  public static CarAccess carAccessForTrip(Trip trip) {
    return trip.getCarsAllowed();
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

    if (requireCarsAllowed) {
      if (carAccessForTrip(trip) != CarAccess.ALLOWED) {
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
    // to this if we have mode specific filters, and not all trips on the pattern have the same
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
  public BitSet filterAvailableStops(
    RoutingTripPattern tripPattern,
    BitSet boardingPossible,
    BoardAlight boardAlight
  ) {
    var result = boardingPossible;

    // if the user wants to include realtime cancellations, include cancelled stops as available
    if (includeRealtimeCancellations) {
      var pattern = tripPattern.getPattern();
      var nStops = pattern.numberOfStops();
      result = new BitSet(nStops);

      for (int i = 0; i < nStops; i++) {
        PickDrop pickDrop =
          switch (boardAlight) {
            case BOARD -> pattern.getBoardType(i);
            case ALIGHT -> pattern.getAlightType(i);
          };
        result.set(i, pickDrop.isRoutable() || pickDrop.is(PickDrop.CANCELLED));
      }
    }

    // if the user wants wheelchair-accessible routes and the configuration requires us to only
    // consider those stops which have the correct accessibility values then use only this for
    // checking whether to board/alight
    if (wheelchairEnabled && wheelchairPreferences.stop().onlyConsiderAccessible()) {
      result = (BitSet) result.clone();
      // Use the and bitwise operator to add false flag to all stops that are not accessible by wheelchair
      result.and(tripPattern.getWheelchairAccessible());

      return result;
    }
    return result;
  }
}
