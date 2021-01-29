package org.opentripplanner.routing.algorithm.raptor.router;


import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * <p>
 * In OTP the street search and transit search is done as to separate searches, then the results
 * are merged and filtered to remove none optimal itineraries. But, when the client do NOT provide
 * a ´directMode´ OTP do not do the streetSearch and the removal of none optimal results do not
 * happen. In other words OTP is forced to return at least one itinerary with at least one transit
 * leg. So, instead of walking maybe 100 meters, the OTP suggest you need to walk to the closest
 * buss stop, take the bus on stop and walk back - with much more walking.
 * <p>
 * <b>Let say OTP produces these internal results:</b>
 * <ul>
 *   <li>Itinerary-1: {@code Origin ~ Walk 100m ~ Destination} (Street search)</li>
 *   <li>Itinerary-2: {@code Origin ~ Walk 120m ~ Stop A ~ Bus 10:00 10:05 ~ Stop B ~ Walk 200m ~ Destination} (Raptor search)</li>
 * </ul>
 * <p>
 * <b>API level results</b>
 * <ul>
 *   <li>Request 1: {@code directMode:WALK, access/egressMode: WALK, transitModes:[TRANSIT] }, OTP returns [ Itinerary-1 ]</li>
 *   <li>Request 2: {@code directMode:null, access/egressMode: WALK, transitModes:[TRANSIT] }, OTP returns [ Itinerary-2 ]</li>
 * </ul>
 * <p>
 * In this case Itinerary-1 is clearly better in all ways compared to Itinerary-2. Some OTP clients
 * run a separate searches for WALK-ALL-THE_WAY, BIKE-ALL-THE-WAY, and WALK+TRANSIT. In this case
 * it would be best for the client that Request 2 above just return an empty set, not the silly
 * Itinerary-2.
 * <p>
 * If no directMode is set, the responsibility of this class it to always filter away itineraries
 * with a generalized-cost that is higher than the WALK-ALL-THE-WAY. We achieve this to set the
 * directMode before searching. This trigger the direct street search, and later the result is
 * passed into the filter chain where none optimal results are removed. Last, this class remove
 * any results produced by the direct street search.
 * <p>
 * <b>Why do we use generalized-cost, and not walk distance or a full pareto comparison?</b>
 * <p>
 * In general we assume that walking can be time-shifted; hence we can exclude arrival and
 * departure time from the comparison. The transit might have a higher cost and shorter travel time
 * than the walk-all-the-way itinerary, but this is theoretical and I have not managed to construct
 * a good transit alternative.
 * <p>
 * If we compared just on walking distance we would still get silly results. E.g. if the only stop
 * reachable within walking distance is a train station, we could get a itinerary like this:
 * <p>
 * {@code Origin ~ Walk 50m ~ Stop 1a ~ Train 10:00 10:15 ~ Stop B ~ Train 10:20 10:35 ~ Stop 1d ~
 * Walk 50m ~ Destination }
 * NOT: {@code Origin ~ Walk 120m ~ Destination }
 */
public class FilterTransitWhenDirectModeIsEmpty {
  private final StreetMode originalDirectMode;

  public FilterTransitWhenDirectModeIsEmpty(RequestModes modes) {
    this.originalDirectMode = modes.directMode;
  }

  public StreetMode resolveDirectMode() {
    return directSearchEmpty() ? StreetMode.WALK : originalDirectMode;
  }

  public boolean removeWalkAllTheWayResults() {
    return directSearchEmpty();
  }

  public StreetMode originalDirectMode() {
    return originalDirectMode;
  }

  private boolean directSearchEmpty() {
    return originalDirectMode == null;
  }
}
