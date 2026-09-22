package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-filters carpool trip candidates based on time compatibility with the passenger request.
 * <p>
 * A trip is rejected when it cannot serve the passenger inside the passenger's window. Trips that
 * pass are still subject to expensive routing and a tighter post-filter on the actual itinerary
 * times.
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
 * <h3>The trip</h3>
 *
 * The insertion evaluation times an insertion from the trip's start and OTP's routed driving
 * times, so the bounds are taken in the same terms:
 *
 * <ul>
 *   <li><strong>{@code tripStart}</strong> — the trip's start time: no pickup or dropoff happens
 *       before it.</li>
 *   <li><strong>{@code tripEnd}</strong> — {@code tripStart} plus the routed baseline to the
 *       destination, with the dwell at each intermediate stop, plus the destination's deviation
 *       budget: no insertion delays the destination beyond its budget, so no pickup or dropoff
 *       happens after it. The end time of the trip data is not used: the routed baseline may take
 *       longer than the driver's schedule.</li>
 * </ul>
 *
 * <h3>The passenger</h3>
 *
 * <ul>
 *   <li><strong>{@code Wp}</strong>, <strong>{@code Wd}</strong> — the passenger's walk from the
 *       origin to the snapped pickup and from the snapped dropoff to the destination; zero when
 *       there is none, or when that end of the ride is a transit stop.</li>
 *   <li><strong>{@code J}</strong> = {@link CarpoolingRequest#getMaxJourneyDuration()} — bounds
 *       the transit ride of unknown duration that separates the carpool from the request anchor
 *       for access with arriveBy=true and egress with arriveBy=false.</li>
 * </ul>
 *
 * <h2>Rules</h2>
 *
 * The request bounds when the car can be at the passenger's own end of the ride; the trip is
 * rejected when {@code [tripStart, tripEnd]} does not overlap that interval:
 *
 * <pre>
 * | Leg type | arriveBy | Reject as TOO EARLY      | Reject as TOO LATE       |
 * |----------|----------|--------------------------|--------------------------|
 * | Direct   | false    | tripEnd &lt; EDT + Wp        | tripStart &gt; LDT + Wp      |
 * | Direct   | true     | tripEnd &lt; EAT − Wd        | tripStart &gt; LAT − Wd      |
 * | Access   | false    | tripEnd &lt; EDT + Wp        | tripStart &gt; LDT + Wp      |
 * | Access   | true     | tripEnd &lt; EAT − J + Wp    | tripStart &gt; LAT           |
 * | Egress   | false    | tripEnd &lt; EDT             | tripStart &gt; LDT + J − Wd  |
 * | Egress   | true     | tripEnd &lt; EAT − Wd        | tripStart &gt; LAT − Wd      |
 * </pre>
 *
 * <h3>Why these shapes</h3>
 *
 * <ul>
 *   <li>Depart-after with the passenger at the pickup (direct, access): the passenger leaves the
 *       origin between EDT and LDT and is picked up {@code Wp} later.</li>
 *   <li>Arrive-by with the passenger at the dropoff (direct, egress): the passenger arrives
 *       between EAT and LAT and is dropped off {@code Wd} earlier.</li>
 *   <li>Egress with arriveBy=false: the passenger reaches the carpool by transit after leaving at
 *       EDT or later, and is dropped off in time to arrive within {@code J} of LDT.</li>
 *   <li>Access with arriveBy=true: the passenger is picked up before arriving by LAT, and, the
 *       journey lasting at most {@code J}, after leaving at {@code EAT − J} or later and walking
 *       to the pickup.</li>
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

  @Override
  public boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger) {
    var request = passenger.request();
    var requestedDateTime = request.getRequestedDateTime();
    if (requestedDateTime == null) {
      return true;
    }

    var tripStart = trip.trip().startTime().toInstant();
    var tripEnd = tripEnd(trip, request.getStopDuration());

    var window = request.isArriveByRequest()
      ? arriveByWindow(passenger, requestedDateTime)
      : departAfterWindow(passenger, requestedDateTime);
    if (tripEnd.isBefore(window.earliest())) {
      return reject(trip.trip(), "tripEnd", tripEnd, "is before", window.earliest());
    }
    if (tripStart.isAfter(window.latest())) {
      return reject(trip.trip(), "tripStart", tripStart, "is after", window.latest());
    }
    return true;
  }

  /**
   * The latest moment the insertion evaluation can time a pickup or dropoff on the trip: its start,
   * plus the routed baseline to the destination with the dwell at each intermediate stop, plus the
   * destination's deviation budget. Package-private for testing.
   */
  static Instant tripEnd(RoutableCarpoolTrip trip, Duration stopDuration) {
    var corridor = trip.corridor();
    var end = trip.trip().startTime().toInstant();
    for (var leg : corridor.legDurations()) {
      end = end.plus(leg);
    }
    end = end.plus(stopDuration.multipliedBy(corridor.legCount() - 1));
    return end.plus(trip.trip().stops().getLast().getDeviationBudget());
  }

  /** {@code arriveBy = false}: requestedDateTime is EDT; LDT = EDT + searchWindow. */
  private static Window departAfterWindow(SnappedPassenger passenger, Instant edt) {
    var request = passenger.request();
    var ldt = edt.plus(request.getSearchWindow());
    if (request.isEgressRequest()) {
      var latest = ldt.plus(request.getMaxJourneyDuration()).minus(passenger.walkFromDropoff());
      return new Window(edt, latest);
    }
    var walk = passenger.walkToPickup();
    return new Window(edt.plus(walk), ldt.plus(walk));
  }

  /** {@code arriveBy = true}: requestedDateTime is LAT; EAT = LAT − searchWindow. */
  private static Window arriveByWindow(SnappedPassenger passenger, Instant lat) {
    var request = passenger.request();
    var eat = lat.minus(request.getSearchWindow());
    if (request.isAccessRequest()) {
      var earliest = eat.minus(request.getMaxJourneyDuration()).plus(passenger.walkToPickup());
      return new Window(earliest, lat);
    }
    var walk = passenger.walkFromDropoff();
    return new Window(eat.minus(walk), lat.minus(walk));
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

  /** When the car can be at the passenger's own end of the ride. */
  private record Window(Instant earliest, Instant latest) {}
}
