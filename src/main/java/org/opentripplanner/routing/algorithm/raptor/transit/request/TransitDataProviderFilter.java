package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Used to filter the elements in a {@link org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer}
 * when constructing a {@link RaptorRoutingRequestTransitData} for a request.
 * <p>
 * {@link TripPatternForDate} and {@link TripTimes} are filtered based on the request parameters to
 * only included components which are allowed by the request. Such filters may included bike or
 * wheelchair accessibility, banned routes and transit modes.
 *
 * @see RoutingRequestTransitDataProviderFilter
 */
public interface TransitDataProviderFilter {

  boolean tripPatternPredicate(TripPatternForDate tripPatternForDate);

  boolean tripTimesPredicate(TripTimes tripTimes);
}
