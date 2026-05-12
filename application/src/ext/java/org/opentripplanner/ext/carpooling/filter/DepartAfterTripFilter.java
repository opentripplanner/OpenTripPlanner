package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-filters trip candidates based on estimated time compatibility for depart-after requests.
 * <p>
 * Because passengers board somewhere along the driver's route rather than at its start, this
 * filter is intentionally loose — only trips that are provably incompatible are discarded.
 * <ul>
 *   <li>{@code trip.endTime >= T} — a trip that has already ended cannot pick up the
 *       passenger.</li>
 *   <li>{@code trip.startTime <= T + searchWindow + maxWalkTime} — a trip starting too far in
 *       the future is out of range. An extra 15-minute walk buffer accounts for the passenger
 *       walking to the pickup point.</li>
 * </ul>
 * Arrive-by requests are passed through unconditionally; their pre-filtering is handled by
 * {@link ArriveByTripFilter}. Tight enforcement of actual itinerary start times is delegated
 * to the post-filter.
 */
public class DepartAfterTripFilter implements CarpoolTripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DepartAfterTripFilter.class);

  @Override
  public boolean isCandidateTrip(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    if (request.isArriveByRequest() || request.getRequestedDateTime() == null) {
      return true;
    }
    var departureTime = request.getRequestedDateTime();
    var slack = request.isEgressRequest() ? MAX_WALK_TIME.plus(EGRESS_SLACK) : MAX_WALK_TIME;
    var upperBound = searchWindow != null ? departureTime.plus(searchWindow).plus(slack) : null;
    return tripIsCandidate(trip, departureTime, upperBound);
  }

  /**
   * Accepts trips still running at {@code departureTime} and starting at or before
   * {@code upperBound}.
   * <p>
   * Using {@code trip.endTime} as the lower bound (rather than {@code trip.startTime}) allows
   * mid-route pickups for trips already underway when the passenger wants to depart.
   */
  private boolean tripIsCandidate(CarpoolTrip trip, Instant departureTime, Instant upperBound) {
    var tripEndTime = trip.endTime() != null ? trip.endTime().toInstant() : null;
    boolean tripStillRunning = tripEndTime == null || !tripEndTime.isBefore(departureTime);
    boolean startsWithinWindow =
      upperBound == null || !trip.startTime().toInstant().isAfter(upperBound);
    boolean accepted = tripStillRunning && startsWithinWindow;
    if (!accepted) {
      LOG.debug(
        "Trip {} rejected by depart-after pre-filter: trip runs {}–{}, passenger requests depart-after {} (upperBound={})",
        trip.getId(),
        trip.startTime().toInstant(),
        tripEndTime,
        departureTime,
        upperBound
      );
    }
    return accepted;
  }
}
