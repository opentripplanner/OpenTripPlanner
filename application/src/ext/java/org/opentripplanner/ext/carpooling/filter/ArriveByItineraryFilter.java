package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.model.plan.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-filter that rejects carpool itineraries arriving after the passenger's requested arrival
 * time.
 * <p>
 * Only active for arrive-by requests; depart-after itineraries are always accepted.
 * <ul>
 *   <li>Direct and access legs: {@code itinerary.endTime <= T}.</li>
 *   <li>Egress legs: {@code itinerary.endTime <= T + 24 h}, because the transit arrival that
 *       triggers the egress leg occurs after T and the exact time is not known here.</li>
 * </ul>
 */
public class ArriveByItineraryFilter implements CarpoolItineraryFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ArriveByItineraryFilter.class);

  @Override
  public boolean isValidItinerary(
    Itinerary itinerary,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    if (!request.isArriveByRequest() || request.getRequestedDateTime() == null) {
      return true;
    }

    Instant deadline = request.isEgressRequest()
      ? request.getRequestedDateTime().plus(CarpoolTripFilter.EGRESS_SLACK)
      : request.getRequestedDateTime();

    var endTime = itinerary.endTimeAsInstant();
    var arrivesOnTime = !endTime.isAfter(deadline);

    if (!arrivesOnTime) {
      LOG.debug(
        "Itinerary {} rejected by arrive-by post-filter: arrives at {}, deadline is {}",
        itinerary.keyAsString(),
        endTime,
        deadline
      );
    }

    return arrivesOnTime;
  }
}
