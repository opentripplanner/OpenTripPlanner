package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-filters carpool trip candidates based on time compatibility with the passenger request.
 * <p>
 * A trip is rejected when it is outside the passenger's window. Trips that pass are still subject
 * to expensive routing and a tighter post-filter on the actual itinerary times.
 *
 * <h2>Variables</h2>
 *
 * <h3>Request window</h3>
 *
 * The passenger's window is derived from {@code request.getRequestedDateTime()} and
 * {@code request.getSearchWindow()}. Which two of EDT/LDT/EAT/LAT exist depends on
 * {@code arriveBy}:
 *
 * <ul>
 *   <li><strong>EDT</strong> — earliest departure time. Defined when {@code arriveBy = false} as
 *       {@code requestedDateTime}. The passenger does not depart from origin before EDT.</li>
 *   <li><strong>LDT</strong> — latest departure time. Defined when {@code arriveBy = false} as
 *       {@code requestedDateTime + searchWindow}. The passenger departs by LDT.</li>
 *   <li><strong>LAT</strong> — latest arrival time. Defined when {@code arriveBy = true} as
 *       {@code requestedDateTime}. The passenger arrives at destination by LAT.</li>
 *   <li><strong>EAT</strong> — earliest arrival time. Defined when {@code arriveBy = true} as
 *       {@code requestedDateTime − searchWindow}. The passenger does not arrive before EAT.</li>
 * </ul>
 *
 * <h3>Slack constants</h3>
 *
 * The carpool window is {@code [trip.startTime, trip.endTime]} but the passenger boards and
 * alights somewhere in the middle. Two slacks pad the request window outward so trips that could
 * still pick up or drop off a passenger inside their window are not rejected:
 *
 * <ul>
 *   <li><strong>{@code W} = {@link CarpoolingRequest#getMaxWalkTime()}</strong> (max walk time) —
 *       upper bound on how long the passenger walks between origin/destination and the carpool
 *       pickup/dropoff. Used wherever a single walk segment shifts the relevant bound by a known,
 *       bounded amount: a passenger leaving by LDT can be at the pickup as late as
 *       {@code LDT + W}; a carpool that drops off by {@code EAT − W} still gives an arrival at or
 *       after EAT after a W-long walk to destination.</li>
 *   <li><strong>{@code T} = {@link #MAX_TOTAL_TRAVEL_TIME}</strong> (max total passenger travel
 *       time, fallback) — used <em>only</em> in the two cells where neither the request window nor
 *       {@code W} can produce a real bound: <em>access / arriveBy=true / too early</em> and
 *       <em>egress / arriveBy=false / too late</em>. In both cases the carpool sits on the
 *       opposite end of the journey from the request anchor, separated from it by a transit ride
 *       of unknown duration. {@code T} caps that unknown duration with a deliberately
 *       conservative number so the filter degrades to "almost a no-op" in those cells rather than
 *       producing false negatives.</li>
 * </ul>
 *
 * <h2>Rules</h2>
 *
 * <pre>
 * | Leg type | arriveBy | Reject as TOO EARLY    | Reject as TOO LATE     |
 * |----------|----------|------------------------|------------------------|
 * | Direct   | false    | tripEnd   &lt; EDT        | tripStart &gt; LDT + W    |
 * | Direct   | true     | tripEnd   &lt; EAT − W    | tripStart &gt; LAT        |
 * | Access   | false    | tripEnd   &lt; EDT        | tripStart &gt; LDT + W    |
 * | Access   | true     | tripEnd   &lt; EAT − T    | tripStart &gt; LAT        |
 * | Egress   | false    | tripEnd   &lt; EDT        | tripStart &gt; LDT + T    |
 * | Egress   | true     | tripEnd   &lt; EAT − W    | tripStart &gt; LAT        |
 * </pre>
 *
 * <h3>Why these shapes</h3>
 *
 * <ul>
 *   <li><em>Too late</em> compares {@code tripStart} (earliest moment any pickup along the route
 *       can happen) against the latest the passenger could be at that pickup. {@code W} is added
 *       when the request anchors departure (arriveBy=false) and a walk pads the bound; {@code T}
 *       replaces {@code W} on the egress/false cell because access + transit between the anchor
 *       and the egress carpool is otherwise unbounded.</li>
 *   <li><em>Too early</em> compares {@code tripEnd} (latest moment any pickup or dropoff along
 *       the route can happen) against the earliest the passenger could be there. {@code W} is
 *       subtracted when a single dropoff walk separates the carpool from the destination anchor
 *       (direct & egress with arriveBy=true); {@code T} replaces {@code W} on the access/true
 *       cell because transit + egress between the carpool dropoff and the anchor is otherwise
 *       unbounded.</li>
 *   <li>The four cells with neither {@code W} nor {@code T} are the cases where the carpool is on
 *       the same side as the anchor and no walk or padding can move the bound.</li>
 * </ul>
 *
 * <h3>Behavior with missing inputs</h3>
 *
 * <ul>
 *   <li>{@code requestedDateTime == null}: pass-through (no filtering possible).</li>
 * </ul>
 */
public class TimeTripFilter implements CarpoolTripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(TimeTripFilter.class);

  /**
   * Conservative cap on total passenger travel time, used as a fallback bound when an unbounded
   * transit segment separates the carpool leg from the request anchor.
   */
  private static final Duration MAX_TOTAL_TRAVEL_TIME = Duration.ofHours(24);

  @Override
  public boolean isCandidateTrip(CarpoolTrip trip, CarpoolingRequest request) {
    var requestedDateTime = request.getRequestedDateTime();
    if (requestedDateTime == null) {
      return true;
    }

    var tripStart = trip.startTime().toInstant();
    var tripEnd = trip.latestEndTime().toInstant();

    return request.isArriveByRequest()
      ? acceptsArriveBy(trip, request, requestedDateTime, tripStart, tripEnd)
      : acceptsDepartAfter(trip, request, requestedDateTime, tripStart, tripEnd);
  }

  /**
   * Rules for {@code arriveBy = true}: requestedDateTime is LAT; EAT = LAT − searchWindow.
   * <ul>
   *   <li>Too late: {@code tripStart > LAT} for all leg types.</li>
   *   <li>Too early: {@code tripEnd < EAT − maxConnectionTime} where maxConnectionTime is {@code T}
   *       for access (transit + egress on the destination side is unbounded) and {@code W}
   *       otherwise.</li>
   * </ul>
   */
  private static boolean acceptsArriveBy(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Instant lat,
    Instant tripStart,
    Instant tripEnd
  ) {
    if (tripStart.isAfter(lat)) {
      return reject(trip, "tripStart", tripStart, "is after LAT", lat);
    }
    var maxConnectionTime = request.isAccessRequest()
      ? MAX_TOTAL_TRAVEL_TIME
      : request.getMaxWalkTime();
    var earliestAcceptableTripEnd = lat.minus(request.getSearchWindow()).minus(maxConnectionTime);
    if (tripEnd.isBefore(earliestAcceptableTripEnd)) {
      return reject(
        trip,
        "tripEnd",
        tripEnd,
        "is before EAT − maxConnectionTime",
        earliestAcceptableTripEnd
      );
    }
    return true;
  }

  /**
   * Rules for {@code arriveBy = false}: requestedDateTime is EDT; LDT = EDT + searchWindow.
   * <ul>
   *   <li>Too early: {@code tripEnd < EDT} for all leg types.</li>
   *   <li>Too late: {@code tripStart > LDT + maxConnectionTime} where maxConnectionTime is
   *       {@code T} for egress (access + transit on the origin side is unbounded) and {@code W}
   *       otherwise.</li>
   * </ul>
   */
  private static boolean acceptsDepartAfter(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Instant edt,
    Instant tripStart,
    Instant tripEnd
  ) {
    if (tripEnd.isBefore(edt)) {
      return reject(trip, "tripEnd", tripEnd, "is before EDT", edt);
    }
    var maxConnectionTime = request.isEgressRequest()
      ? MAX_TOTAL_TRAVEL_TIME
      : request.getMaxWalkTime();
    var latestAcceptableTripStart = edt.plus(request.getSearchWindow()).plus(maxConnectionTime);
    if (tripStart.isAfter(latestAcceptableTripStart)) {
      return reject(
        trip,
        "tripStart",
        tripStart,
        "is after LDT + maxConnectionTime",
        latestAcceptableTripStart
      );
    }
    return true;
  }

  private static boolean reject(
    CarpoolTrip trip,
    String field,
    Instant value,
    String reason,
    Instant bound
  ) {
    LOG.debug("Trip {} rejected: {} {} {} {}", trip.getId(), field, value, reason, bound);
    return false;
  }
}
