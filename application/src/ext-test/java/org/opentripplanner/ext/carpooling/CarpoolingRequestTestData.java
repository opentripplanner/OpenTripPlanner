package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequestBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Factory methods for creating {@link CarpoolingRequest} instances in tests.
 * <p>
 * Every factory pins the same {@link #DEFAULT_MAX_WALK_TIME}, {@link #DEFAULT_SEARCH_WINDOW},
 * {@link #DEFAULT_STOP_DURATION} and {@link #DEFAULT_MAX_JOURNEY_DURATION} so boundary assertions
 * in filter tests stay independent of OTP defaults.
 */
public class CarpoolingRequestTestData {

  /** The maximum walk the requests carry. */
  public static final Duration DEFAULT_MAX_WALK_TIME = Duration.ofMinutes(15);

  /** 30-minute window the {@code TimeTripFilter} / {@code TimeItineraryFilter} tests use. */
  public static final Duration DEFAULT_SEARCH_WINDOW = Duration.ofMinutes(30);

  /** The dwell at each stop of a trip, the passenger's included. */
  public static final Duration DEFAULT_STOP_DURATION = Duration.ofMinutes(1);

  /** The longest journey the requests accept, OTP's default. */
  public static final Duration DEFAULT_MAX_JOURNEY_DURATION = Duration.ofHours(24);

  /** Direct routing request with specific passenger coordinates; no time constraint. */
  public static CarpoolingRequest directRequest(WgsCoordinate pickup, WgsCoordinate dropoff) {
    return baseBuilder().withPassengerPickup(pickup).withPassengerDropoff(dropoff).build();
  }

  public static CarpoolingRequest departAfterWithNoTime() {
    return baseBuilder().withArriveBy(false).build();
  }

  public static CarpoolingRequest arriveByWithNoTime() {
    return baseBuilder().withArriveBy(true).build();
  }

  public static CarpoolingRequest departAfterDirect(Instant T) {
    return request(T, false, null);
  }

  public static CarpoolingRequest departAfterAccess(Instant T) {
    return request(T, false, AccessEgressType.ACCESS);
  }

  public static CarpoolingRequest departAfterEgress(Instant T) {
    return request(T, false, AccessEgressType.EGRESS);
  }

  public static CarpoolingRequest arriveByDirect(Instant T) {
    return request(T, true, null);
  }

  public static CarpoolingRequest arriveByAccess(Instant T) {
    return request(T, true, AccessEgressType.ACCESS);
  }

  public static CarpoolingRequest arriveByEgress(Instant T) {
    return request(T, true, AccessEgressType.EGRESS);
  }

  private static CarpoolingRequest request(
    Instant T,
    boolean arriveBy,
    AccessEgressType accessOrEgress
  ) {
    return baseBuilder()
      .withArriveBy(arriveBy)
      .withRequestedDateTime(T)
      .withAccessOrEgress(accessOrEgress)
      .build();
  }

  private static CarpoolingRequestBuilder baseBuilder() {
    return new CarpoolingRequestBuilder()
      .withMaxWalkTime(DEFAULT_MAX_WALK_TIME)
      .withSearchWindow(DEFAULT_SEARCH_WINDOW)
      .withStopDuration(DEFAULT_STOP_DURATION)
      .withMaxJourneyDuration(DEFAULT_MAX_JOURNEY_DURATION);
  }

  private CarpoolingRequestTestData() {}
}
