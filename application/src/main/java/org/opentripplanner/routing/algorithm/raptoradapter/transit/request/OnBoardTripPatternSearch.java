package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Provides search operations over the active Raptor pattern index for on-board trip resolution.
 * Encapsulates the pattern-index data structure and exposes query methods needed by
 * {@link org.opentripplanner.routing.algorithm.raptoradapter.router.OnBoardAccessResolver}.
 */
public class OnBoardTripPatternSearch {

  private final List<TripPatternForDates> patternIndex;

  public OnBoardTripPatternSearch(List<TripPatternForDates> patternIndex) {
    this.patternIndex = patternIndex;
  }

  /**
   * Check whether the given trip pattern's route index exists in the active pattern index.
   */
  public boolean isInPatternIndex(TripPattern pattern) {
    int routeIndex = pattern.getRoutingTripPattern().patternIndex();
    return routeIndex < patternIndex.size() && patternIndex.get(routeIndex) != null;
  }

  /**
   * Get the {@link TripPatternForDates} for a given route index.
   *
   * @throws IllegalArgumentException if the route index is out of bounds or not active
   */
  public TripPatternForDates findTripPatternForDates(int routeIndex) {
    if (routeIndex >= patternIndex.size() || patternIndex.get(routeIndex) == null) {
      throw new IllegalArgumentException(
        "Route index %d not found in active patterns for this search".formatted(routeIndex)
      );
    }
    return patternIndex.get(routeIndex);
  }

  /**
   * Find the global trip schedule index within the {@link TripPatternForDates} for a specific trip
   * on a specific service date.
   *
   * @throws IllegalArgumentException if the trip or service date is not found
   */
  public int findTripScheduleIndex(
    TripPatternForDates tripPatternForDates,
    Trip trip,
    LocalDate serviceDate
  ) {
    int globalIndex = 0;
    var dateIterator = tripPatternForDates.tripPatternForDatesIndexIterator(true);

    while (dateIterator.hasNext()) {
      int dayIndex = dateIterator.next();
      TripPatternForDate tripPatternForDate = tripPatternForDates.tripPatternForDate(dayIndex);

      if (tripPatternForDate.getServiceDate().equals(serviceDate)) {
        for (int i = 0; i < tripPatternForDate.numberOfTripSchedules(); i++) {
          if (tripPatternForDate.getTripTimes(i).getTrip().getId().equals(trip.getId())) {
            return globalIndex + i;
          }
        }
        throw new IllegalArgumentException(
          "Trip %s not found in pattern for service date %s".formatted(trip.getId(), serviceDate)
        );
      }
      globalIndex += tripPatternForDate.numberOfTripSchedules();
    }

    throw new IllegalArgumentException(
      "Service date %s not found in active patterns for trip %s".formatted(
        serviceDate,
        trip.getId()
      )
    );
  }
}
