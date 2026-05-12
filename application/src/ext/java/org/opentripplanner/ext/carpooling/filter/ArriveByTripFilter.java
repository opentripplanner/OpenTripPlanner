package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-filters trip candidates based on estimated time compatibility for arrive-by requests.
 * <p>
 * Because the passenger is dropped off somewhere along the driver's route rather than at its end,
 * this filter is intentionally loose — only trips that are provably incompatible are discarded.
 * <ul>
 *   <li>{@code trip.startTime <= T} — a driver who has not yet started cannot drop off the
 *       passenger in time. For egress legs, the deadline is relaxed to {@code T + 24 h} because
 *       the actual transit arrival time is unknown at pre-filter time.</li>
 * </ul>
 * Depart-after requests are passed through unconditionally; their pre-filtering is handled by
 * {@link DepartAfterTripFilter}. Tight enforcement of actual itinerary end times is delegated
 * to the post-filter.
 */
public class ArriveByTripFilter implements CarpoolTripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ArriveByTripFilter.class);

  @Override
  public boolean isCandidateTrip(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    if (!request.isArriveByRequest() || request.getRequestedDateTime() == null) {
      return true;
    }
    var arrivalTime = request.getRequestedDateTime();
    var deadline = request.isEgressRequest() ? arrivalTime.plus(EGRESS_SLACK) : arrivalTime;
    return tripStartsAtOrBefore(trip, deadline);
  }

  private boolean tripStartsAtOrBefore(CarpoolTrip trip, Instant deadline) {
    var tripStartTime = trip.startTime().toInstant();
    boolean accepted = !tripStartTime.isAfter(deadline);
    if (!accepted) {
      LOG.debug(
        "Trip {} rejected by arrive-by pre-filter: trip starts at {}, which is after deadline {}",
        trip.getId(),
        tripStartTime,
        deadline
      );
    }
    return accepted;
  }
}
