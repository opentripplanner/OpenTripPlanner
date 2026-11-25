package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.routing.CarpoolingRequest;
import org.opentripplanner.model.plan.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-filter that rejects carpool itineraries whose times fall outside the passenger's window.
 * {@link TimeTripFilter} bounds a trip loosely before routing; this filter sees the routed
 * itinerary and enforces the window exactly. With EDT, LDT, EAT and LAT as defined there:
 *
 * <pre>
 * | arriveBy | Reject when itinerary.startTime | Reject when itinerary.endTime |
 * |----------|---------------------------------|-------------------------------|
 * | false    | ∉ [EDT, LDT]                    | —                             |
 * | true     | —                               | ∉ [EAT, LAT]                  |
 * </pre>
 *
 * The walks are part of the itinerary's times, so no slack is added. Every itinerary passes when
 * the request has no time.
 */
public class TimeItineraryFilter implements CarpoolItineraryFilter {

  private static final Logger LOG = LoggerFactory.getLogger(TimeItineraryFilter.class);

  @Override
  public boolean isValidItinerary(Itinerary itinerary, CarpoolingRequest request) {
    var requestedDateTime = request.getRequestedDateTime();
    if (requestedDateTime == null) {
      return true;
    }

    return request.isArriveByRequest()
      ? acceptsArriveBy(itinerary, requestedDateTime, request.getSearchWindow())
      : acceptsDepartAfter(itinerary, requestedDateTime, request.getSearchWindow());
  }

  /**
   * Rules for {@code arriveBy = true}: requestedDateTime is LAT; EAT = LAT − searchWindow.
   * Itinerary endTime must lie in {@code [EAT, LAT]}.
   */
  private static boolean acceptsArriveBy(Itinerary itinerary, Instant lat, Duration searchWindow) {
    var endTime = itinerary.endTimeAsInstant();
    if (endTime.isAfter(lat)) {
      return reject(itinerary, "endTime", endTime, "is after LAT", lat);
    }
    var eat = lat.minus(searchWindow);
    if (endTime.isBefore(eat)) {
      return reject(itinerary, "endTime", endTime, "is before EAT", eat);
    }
    return true;
  }

  /**
   * Rules for {@code arriveBy = false}: requestedDateTime is EDT; LDT = EDT + searchWindow.
   * Itinerary startTime must lie in {@code [EDT, LDT]}.
   */
  private static boolean acceptsDepartAfter(
    Itinerary itinerary,
    Instant edt,
    Duration searchWindow
  ) {
    var startTime = itinerary.startTimeAsInstant();
    if (startTime.isBefore(edt)) {
      return reject(itinerary, "startTime", startTime, "is before EDT", edt);
    }
    var ldt = edt.plus(searchWindow);
    if (startTime.isAfter(ldt)) {
      return reject(itinerary, "startTime", startTime, "is after LDT", ldt);
    }
    return true;
  }

  private static boolean reject(
    Itinerary itinerary,
    String field,
    Instant value,
    String reason,
    Instant bound
  ) {
    LOG.debug(
      "Itinerary {} rejected: {} {} {} {}",
      itinerary.keyAsString(),
      field,
      value,
      reason,
      bound
    );
    return false;
  }
}
