package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.model.plan.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-filter that rejects fully-routed carpool itineraries whose actual times fall outside the
 * passenger's requested window.
 * <p>
 * Unlike {@link TimeTripFilter}, which screens raw {@link org.opentripplanner.ext.carpooling.model.CarpoolTrip}
 * candidates using loose bounds on {@code tripStart}/{@code tripEnd}, this filter receives the
 * complete {@link Itinerary} with the actual departure and arrival times after routing — and
 * enforces the request window tightly. Today this only runs for direct itineraries (the
 * access/egress flow produces {@code CarpoolAccessEgress} objects, not {@code Itinerary}).
 *
 * <h2>Variables</h2>
 *
 * The passenger's window is derived from {@code request.getRequestedDateTime()} and
 * {@code request.getSearchWindow()}. Which two of EDT/LDT/EAT/LAT exist depends on
 * {@code arriveBy}:
 *
 * <ul>
 *   <li><strong>EDT</strong> — earliest departure time = {@code requestedDateTime}
 *       (when {@code arriveBy = false}).</li>
 *   <li><strong>LDT</strong> — latest departure time = {@code requestedDateTime + searchWindow}
 *       (when {@code arriveBy = false}).</li>
 *   <li><strong>LAT</strong> — latest arrival time = {@code requestedDateTime}
 *       (when {@code arriveBy = true}).</li>
 *   <li><strong>EAT</strong> — earliest arrival time = {@code requestedDateTime − searchWindow}
 *       (when {@code arriveBy = true}).</li>
 * </ul>
 *
 * <h2>Rules</h2>
 *
 * <pre>
 * | arriveBy | Reject when itinerary.startTime | Reject when itinerary.endTime |
 * |----------|---------------------------------|-------------------------------|
 * | false    | ∉ [EDT, LDT]                    | —                             |
 * | true     | —                               | ∉ [EAT, LAT]                  |
 * </pre>
 *
 * The arriveBy=false rules anchor the passenger's departure from origin; the arriveBy=true rules
 * anchor the passenger's arrival at destination. No slack is added on either side: post-filters
 * see actual itinerary times rather than trip-endpoint estimates, so the walk-time padding used
 * by the pre-filter is already baked into {@code startTime}/{@code endTime}. The leg type
 * (direct, access, egress) is irrelevant: identical bounds apply in all cases.
 *
 * <h2>Behavior with missing inputs</h2>
 *
 * <ul>
 *   <li>{@code requestedDateTime == null}: pass-through (no filtering possible).</li>
 * </ul>
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
