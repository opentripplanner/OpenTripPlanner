package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import org.opentripplanner.model.plan.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-filter that rejects carpool itineraries whose actual departure time falls outside the
 * passenger's requested depart-after window.
 * <p>
 * Only active for depart-after requests; arrive-by itineraries are always accepted.
 * <ul>
 *   <li>Lower bound: {@code itinerary.startTime >= T} — the itinerary must not depart before
 *       the requested time.</li>
 *   <li>Upper bound: {@code itinerary.startTime <= T + searchWindow} — the itinerary must not
 *       depart too far into the future.</li>
 * </ul>
 * The upper bound is only enforced when {@code searchWindow} is non-null.
 */
public class DepartAfterItineraryFilter implements CarpoolItineraryFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DepartAfterItineraryFilter.class);

  @Override
  public boolean isValidItinerary(
    Itinerary itinerary,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    if (request.isArriveByRequest() || request.getRequestedDateTime() == null) {
      return true;
    }

    var requestedDepartureTime = request.getRequestedDateTime();
    var startTime = itinerary.startTimeAsInstant();

    if (startTime.isBefore(requestedDepartureTime)) {
      LOG.debug(
        "Itinerary {} rejected by depart-after post-filter: departs at {}, requested depart-after {} — {} too early",
        itinerary.keyAsString(),
        startTime,
        requestedDepartureTime,
        Duration.between(startTime, requestedDepartureTime)
      );
      return false;
    }

    if (searchWindow != null) {
      var upperBound = requestedDepartureTime.plus(searchWindow);
      if (startTime.isAfter(upperBound)) {
        LOG.debug(
          "Itinerary {} rejected by depart-after post-filter: departs at {}, which is after upper bound {}",
          itinerary.keyAsString(),
          startTime,
          upperBound
        );
        return false;
      }
    }

    return true;
  }
}
