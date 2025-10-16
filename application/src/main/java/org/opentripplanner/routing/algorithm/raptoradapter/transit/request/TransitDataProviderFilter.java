package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Used to filter the elements in a {@link RaptorTransitData}
 * when constructing a {@link RaptorRoutingRequestTransitData} for a request.
 * <p>
 * {@link TripPatternForDate} and {@link TripTimes} are filtered based on the request parameters to
 * only included components which are allowed by the request. Such filters may include bike or
 * wheelchair accessibility, banned routes and transit modes.
 *
 * @see RouteRequestTransitDataProviderFilter
 */
public interface TransitDataProviderFilter {
  /**
   * For performance reasons filtering is done in a two-step process. First you apply the pattern. If it doesn't match you
   * get a null value. If it does match you get a Predicate&lt;TripTimes&gt; that you use to match a TripTimes
   * object.
   */
  @Nullable
  Predicate<TripTimes> createTripFilter(TripPattern tripPattern);

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
