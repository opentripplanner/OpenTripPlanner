package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Used to filter the elements in a {@link RaptorTransitData}
 * when constructing a {@link RaptorRoutingRequestTransitData} for a request.
 * <p>
 * {@link TripPatternForDate} and {@link TripTimes} are filtered based on the request parameters to
 * only included components which are allowed by the request. Such filters may included bike or
 * wheelchair accessibility, banned routes and transit modes.
 *
 * @see RouteRequestTransitDataProviderFilter
 */
public interface TransitDataProviderFilter {
  boolean tripPatternPredicate(TripPatternForDate tripPatternForDate);

  boolean hasSubModeFilters();

  boolean tripTimesPredicate(TripTimes tripTimes, boolean withFilters);

  /**
   * Check if boarding/alighting is possible at each stop. If the values differ from the default
   * input values, create a clone of the bitset and subtract the unavailable stops.
   *
   * @param tripPattern      Trip pattern that should contain boarding/alighting information, e.g.
   *                         wheelchair accessibility for each stop.
   * @param boardingPossible Initial information regarding boarding/alighting possible
   * @param boardAlight      Whether we are filtering boarding or alighting stops
   * @return Information if stops are available for boarding or alighting
   */
  BitSet filterAvailableStops(
    RoutingTripPattern tripPattern,
    BitSet boardingPossible,
    BoardAlight boardAlight
  );
}
